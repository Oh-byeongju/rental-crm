package com.rental.crm.billing.service;

import com.rental.crm.billing.dto.BillingResponse;
import com.rental.crm.billing.dto.BillingSearchRequest;
import com.rental.crm.billing.entity.Billing;
import com.rental.crm.billing.repository.BillingRepository;
import com.rental.crm.common.exception.BusinessException;
import com.rental.crm.common.exception.ErrorCode;
import com.rental.crm.contract.entity.Contract;
import com.rental.crm.contract.repository.ContractRepository;
import com.rental.crm.customer.entity.Customer;
import com.rental.crm.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 청구 도메인 — 조회 + 단건 상태 전이 (취소/연체).
 *
 * <p>책임:
 * <ul>
 *   <li>search / findById — 페이지 + 단건 조회 ({@code service-mapping-pattern.md} 적용)</li>
 *   <li>changeDueDate — 납기 연장</li>
 *   <li>cancel — UNPAID/OVERDUE → CANCELLED (allow-list)</li>
 *   <li>markOverdue — UNPAID → OVERDUE (연체 배치에서 호출)</li>
 *   <li>markPaid — UNPAID/OVERDUE → PAID (Ch.3 수납 도메인에서 호출 예정)</li>
 * </ul>
 *
 * <p>⚠️ 배치 트리거 (월 청구 일괄 생성 — Ch.1 핵심) 는 별도 메서드로 추후 추가. 컨테이너 검증 후 작성.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillingService {

    private final BillingRepository billingRepository;
    private final ContractRepository contractRepository;
    private final CustomerRepository customerRepository;

    // ===================== Read =====================

    public Page<BillingResponse> search(BillingSearchRequest req, Pageable pageable) {
        Page<Billing> page = billingRepository.search(
                req.hasBillingNo()     ? req.billingNo()     : null,
                req.hasCustomerId()    ? req.customerId()    : null,
                req.hasContractId()    ? req.contractId()    : null,
                req.hasBillingMonth()  ? req.billingMonth()  : null,
                req.hasBillingStatus() ? req.billingStatus() : null,
                pageable);

        Set<Long> contractIds = page.getContent().stream().map(Billing::getContractId).collect(Collectors.toSet());
        Set<Long> customerIds = page.getContent().stream().map(Billing::getCustomerId).collect(Collectors.toSet());
        Map<Long, Contract> contractMap = contractRepository.findAllById(contractIds).stream()
                .collect(Collectors.toMap(Contract::getContractId, c -> c));
        Map<Long, Customer> customerMap = customerRepository.findAllById(customerIds).stream()
                .collect(Collectors.toMap(Customer::getCustomerId, c -> c));

        return page.map(b -> BillingResponse.from(b, contractMap.get(b.getContractId()), customerMap.get(b.getCustomerId())));
    }

    public BillingResponse findById(Long billingId) {
        return toResponse(loadBilling(billingId));
    }

    // ===================== Update (DUE_DATE) =====================
    @Transactional
    public BillingResponse changeDueDate(Long billingId, LocalDate dueDate) {
        Billing billing = loadBilling(billingId);
        if (Billing.STATUS_PAID.equals(billing.getBillingStatus())
                || Billing.STATUS_CANCELLED.equals(billing.getBillingStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "PAID/CANCELLED 청구는 수정 불가: " + billing.getBillingStatus());
        }
        billing.changeDueDate(dueDate);
        return toResponse(billing);
    }

    // ===================== 상태 전이 =====================

    @Transactional
    public BillingResponse cancel(Long billingId) {
        Billing billing = loadBilling(billingId);
        if (!Billing.CANCELLABLE_STATUS.contains(billing.getBillingStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "취소 불가 상태: " + billing.getBillingStatus());
        }
        billing.cancel();
        return toResponse(billing);
    }

    /** Ch.3 수납 도메인에서 호출 예정 — 수납 완료 시 청구 PAID. */
    @Transactional
    public void markPaid(Long billingId) {
        Billing billing = loadBilling(billingId);
        if (!Billing.PAYABLE_STATUS.contains(billing.getBillingStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "수납 처리 불가 상태: " + billing.getBillingStatus());
        }
        billing.markPaid();
    }

    /** Ch.3 연체 배치에서 호출 예정 — UNPAID + DUE_DATE 지남 → OVERDUE. */
    @Transactional
    public void markOverdue(Long billingId) {
        Billing billing = loadBilling(billingId);
        if (!Billing.OVERDUE_CANDIDATE.contains(billing.getBillingStatus())) {
            return;  // 이미 다른 상태면 무시 (멱등성)
        }
        billing.markOverdue();
    }

    // ===================== Private =====================

    private Billing loadBilling(Long billingId) {
        return billingRepository.findById(billingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "청구 없음: " + billingId));
    }

    private BillingResponse toResponse(Billing billing) {
        Contract contract = contractRepository.findById(billing.getContractId()).orElse(null);
        Customer customer = customerRepository.findById(billing.getCustomerId()).orElse(null);
        return BillingResponse.from(billing, contract, customer);
    }
}
