package com.rental.backoffice.product.service;

import com.rental.domain.common.exception.BusinessException;
import com.rental.domain.common.exception.ErrorCode;
import com.rental.domain.equipment.entity.Equipment;
import com.rental.domain.equipment.repository.EquipmentRepository;
import com.rental.backoffice.product.dto.ProductCreateRequest;
import com.rental.backoffice.product.dto.ProductResponse;
import com.rental.backoffice.product.dto.ProductSearchRequest;
import com.rental.backoffice.product.dto.ProductUpdateRequest;
import com.rental.domain.product.entity.Product;
import com.rental.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 상품 도메인 — 단순 CRUD + EQUIPMENT_ID FK 검증 + UNIQUE 사전 검증.
 *
 * <p>책임:
 * <ul>
 *   <li>CT_PRODUCT CRUD</li>
 *   <li>PRODUCT_CODE UNIQUE 사전 검증 — `backoffice/guide/conventions/api-safety.md §2-3`</li>
 *   <li>EQUIPMENT_ID 존재 검증 — USE_YN 제약 없음 (단종 장비도 상품 등록/유지 가능)</li>
 *   <li>응답 매핑 시 장비 정보 (코드/모델명/제조사/재고) 함께 반환 (그리드/상세 표시용)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final EquipmentRepository equipmentRepository;

    // ===================== Create =====================
    @Transactional
    public ProductResponse register(ProductCreateRequest req) {
        if (productRepository.existsByProductCode(req.productCode())) {
            throw new BusinessException(ErrorCode.ALREADY_EXISTS,
                    "이미 등록된 상품코드: " + req.productCode());
        }
        Equipment equipment = loadEquipment(req.equipmentId());

        var product = Product.builder()
                .productCode(req.productCode())
                .equipmentId(req.equipmentId())
                .productName(req.productName())
                .monthlyFee(req.monthlyFee())
                .contractMonths(req.contractMonths())
                .depositAmount(req.depositAmount())
                .installFee(req.installFee())
                .description(req.description())
                .build();
        var saved = productRepository.save(product);
        return ProductResponse.from(saved, equipment);
    }

    // ===================== Read =====================
    public Page<ProductResponse> search(ProductSearchRequest req, Pageable pageable) {
        Page<Product> page = productRepository.search(
                req.hasProductCode() ? req.productCode() : null,
                req.hasEquipmentId() ? req.equipmentId() : null,
                req.hasProductName() ? req.productName() : null,
                req.hasUseYn()       ? req.useYn()       : null,
                pageable);

        Set<Long> equipmentIds = page.getContent().stream()
                .map(Product::getEquipmentId)
                .collect(Collectors.toSet());
        Map<Long, Equipment> equipmentMap = equipmentRepository.findAllById(equipmentIds).stream()
                .collect(Collectors.toMap(Equipment::getEquipmentId, e -> e));

        return page.map(p -> ProductResponse.from(p, equipmentMap.get(p.getEquipmentId())));
    }

    public ProductResponse findById(Long productId) {
        var product = loadProduct(productId);
        Equipment equipment = equipmentRepository.findById(product.getEquipmentId()).orElse(null);
        return ProductResponse.from(product, equipment);
    }

    // ===================== Update =====================
    @Transactional
    public ProductResponse update(Long productId, ProductUpdateRequest req) {
        var product = loadProduct(productId);

        // PRODUCT_CODE 변경 시 UNIQUE 사전 검증 (본인 제외)
        boolean codeChanged = !product.getProductCode().equals(req.productCode());
        if (codeChanged && productRepository.existsByProductCode(req.productCode())) {
            throw new BusinessException(ErrorCode.ALREADY_EXISTS,
                    "이미 등록된 상품코드: " + req.productCode());
        }
        // EQUIPMENT_ID 변경 시 존재 검증 (USE_YN 무관)
        boolean equipmentChanged = !product.getEquipmentId().equals(req.equipmentId());
        if (equipmentChanged) {
            loadEquipment(req.equipmentId());
        }

        product.changeProductCode(req.productCode());
        product.changeEquipmentId(req.equipmentId());
        product.changeProductName(req.productName());
        product.changeMonthlyFee(req.monthlyFee());
        product.changeContractMonths(req.contractMonths());
        product.changeDepositAmount(req.depositAmount());
        product.changeInstallFee(req.installFee());
        product.changeDescription(req.description());
        product.changeUseYn(req.useYn());

        Equipment equipment = equipmentRepository.findById(req.equipmentId()).orElse(null);
        return ProductResponse.from(product, equipment);
    }

    // ===================== Delete (Soft) =====================
    @Transactional
    public void deactivate(Long productId) {
        var product = loadProduct(productId);
        product.deactivate();
    }

    // ===================== Private =====================
    private Product loadProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "상품 없음: " + productId));
    }

    private Equipment loadEquipment(Long equipmentId) {
        return equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "장비 없음: " + equipmentId));
    }
}
