package com.rental.crm.contract.service;

import com.rental.crm.common.exception.BusinessException;
import com.rental.crm.common.exception.ErrorCode;
import com.rental.crm.contract.dto.ContractActionRequest;
import com.rental.crm.contract.dto.ContractCreateRequest;
import com.rental.crm.contract.dto.ContractResponse;
import com.rental.crm.contract.dto.ContractSearchRequest;
import com.rental.crm.contract.dto.ContractUpdateRequest;
import com.rental.crm.contract.entity.Contract;
import com.rental.crm.contract.repository.ContractRepository;
import com.rental.crm.customer.entity.Customer;
import com.rental.crm.customer.repository.CustomerRepository;
import com.rental.crm.equipment.entity.Equipment;
import com.rental.crm.equipment.repository.EquipmentRepository;
import com.rental.crm.product.entity.Product;
import com.rental.crm.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 계약 도메인 — 가장 복잡한 도메인.
 *
 * <p>책임:
 * <ul>
 *   <li>등록: 고객/상품/장비 존재 검증 + 재고 검증 + MONTHLY_FEE 스냅샷 + END_DATE 자동 계산 + CONTRACT_NO 자동 채번</li>
 *   <li>수정 (END_DATE/INSTALL_ADDRESS): TERMINATED 외 가능. 사실관계 (CUSTOMER/PRODUCT/START/MONTHLY_FEE) 불변</li>
 *   <li>상태 전이 4종: suspend / resume / terminate — allow-list 검증 (delete-defense.md 실전 적용)</li>
 *   <li>응답 매핑: 고객/상품/장비 정보 풀어서 전달</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractService {

    private static final DateTimeFormatter NO_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ContractRepository contractRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final EquipmentRepository equipmentRepository;

    // ===================== Create =====================
    @Transactional
    public ContractResponse register(ContractCreateRequest req) {
        // 1. 존재 검증 (FK)
        Customer customer  = customerRepository.findById(req.customerId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "고객 없음: " + req.customerId()));
        Product  product   = productRepository.findById(req.productId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "상품 없음: " + req.productId()));
        Equipment equipment = equipmentRepository.findById(product.getEquipmentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "장비 없음: " + product.getEquipmentId()));

        // 2. 신규 계약 차단 (D4) — 상품·장비 USE_YN 'Y' AND 활성 계약 수 < STOCK_QTY
        validateStockAvailable(product, equipment);

        // 3. MONTHLY_FEE 스냅샷 (D6), END_DATE 자동 계산 (D7)
        Long monthlyFee   = product.getMonthlyFee();
        LocalDate endDate = req.startDate().plusMonths(product.getContractMonths().longValue());

        // 4. 임시 CONTRACT_NO 로 INSERT — 실제 NO 는 contractId 받은 후 (D1)
        Contract draft = Contract.builder()
                .contractNo("TMP-" + System.nanoTime())
                .customerId(req.customerId())
                .productId(req.productId())
                .monthlyFee(monthlyFee)
                .startDate(req.startDate())
                .endDate(endDate)
                .installAddress(req.installAddress())
                .build();
        Contract saved = contractRepository.save(draft);

        // 5. 자동 채번 적용 (JPA dirty checking → 트랜잭션 끝에 UPDATE)
        saved.assignContractNo(generateContractNo(saved.getContractId()));

        return ContractResponse.from(saved, customer, product, equipment);
    }

    // ===================== Read =====================
    public Page<ContractResponse> search(ContractSearchRequest req, Pageable pageable) {
        Page<Contract> page = contractRepository.search(
                req.hasContractNo()     ? req.contractNo()     : null,
                req.hasCustomerId()     ? req.customerId()     : null,
                req.hasProductId()      ? req.productId()      : null,
                req.hasContractStatus() ? req.contractStatus() : null,
                pageable);

        // 페이지 단위 일괄 매핑 — N+1 회피
        Set<Long> customerIds = page.getContent().stream().map(Contract::getCustomerId).collect(Collectors.toSet());
        Set<Long> productIds  = page.getContent().stream().map(Contract::getProductId).collect(Collectors.toSet());
        Map<Long, Customer> customerMap = customerRepository.findAllById(customerIds).stream()
                .collect(Collectors.toMap(Customer::getCustomerId, c -> c));
        Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p));
        Set<Long> equipmentIds = productMap.values().stream()
                .map(Product::getEquipmentId).collect(Collectors.toSet());
        Map<Long, Equipment> equipmentMap = equipmentRepository.findAllById(equipmentIds).stream()
                .collect(Collectors.toMap(Equipment::getEquipmentId, e -> e));

        return page.map(c -> {
            Product product = productMap.get(c.getProductId());
            Equipment equipment = product == null ? null : equipmentMap.get(product.getEquipmentId());
            return ContractResponse.from(c, customerMap.get(c.getCustomerId()), product, equipment);
        });
    }

    public ContractResponse findById(Long contractId) {
        Contract contract = loadContract(contractId);
        return toResponse(contract);
    }

    // ===================== Update (일반 수정 — END_DATE / INSTALL_ADDRESS) =====================
    @Transactional
    public ContractResponse update(Long contractId, ContractUpdateRequest req) {
        Contract contract = loadContract(contractId);
        if (Contract.STATUS_TERMINATED.equals(contract.getContractStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE, "해지된 계약은 수정할 수 없습니다");
        }
        contract.changeEndDate(req.endDate());
        contract.changeInstallAddress(req.installAddress());
        return toResponse(contract);
    }

    // ===================== 상태 전이 (suspend / resume / terminate) =====================

    @Transactional
    public ContractResponse suspend(Long contractId, ContractActionRequest req) {
        Contract contract = loadContract(contractId);
        if (!Contract.SUSPENDABLE_STATUS.contains(contract.getContractStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "일시정지 불가 상태: " + contract.getContractStatus());
        }
        contract.suspend(req.reason());
        return toResponse(contract);
    }

    @Transactional
    public ContractResponse resume(Long contractId) {
        Contract contract = loadContract(contractId);
        if (!Contract.RESUMABLE_STATUS.contains(contract.getContractStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "재개 불가 상태: " + contract.getContractStatus());
        }
        contract.resume();
        return toResponse(contract);
    }

    @Transactional
    public ContractResponse terminate(Long contractId, ContractActionRequest req) {
        Contract contract = loadContract(contractId);
        if (!Contract.TERMINATABLE_STATUS.contains(contract.getContractStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "해지 불가 상태: " + contract.getContractStatus());
        }
        contract.terminate(req.reason());
        return toResponse(contract);
    }

    // ===================== Private =====================

    private Contract loadContract(Long contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "계약 없음: " + contractId));
    }

    /** `CT-YYYYMMDD-NNNNN` — 등록일 + contractId 5자리 zero-padded. */
    private String generateContractNo(Long contractId) {
        return String.format("CT-%s-%05d", LocalDate.now().format(NO_DATE_FMT), contractId);
    }

    /** 신규 계약 차단 검증 (D4): 상품 USE_YN='Y' AND 장비 USE_YN='Y' AND 활성 계약 수 &lt; STOCK_QTY. */
    private void validateStockAvailable(Product product, Equipment equipment) {
        if (!"Y".equals(product.getUseYn())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "비활성 상품: " + product.getProductCode());
        }
        if (!"Y".equals(equipment.getUseYn())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "단종 장비: " + equipment.getEquipmentCode());
        }
        long activeCount = contractRepository.countActiveByEquipmentId(equipment.getEquipmentId());
        if (activeCount >= equipment.getStockQty()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    String.format("장비 재고 부족: %s (재고 %d, 활성 계약 %d)",
                            equipment.getEquipmentCode(), equipment.getStockQty(), activeCount));
        }
    }

    private ContractResponse toResponse(Contract contract) {
        Customer customer  = customerRepository.findById(contract.getCustomerId()).orElse(null);
        Product  product   = productRepository.findById(contract.getProductId()).orElse(null);
        Equipment equipment = (product == null) ? null
                : equipmentRepository.findById(product.getEquipmentId()).orElse(null);
        return ContractResponse.from(contract, customer, product, equipment);
    }
}
