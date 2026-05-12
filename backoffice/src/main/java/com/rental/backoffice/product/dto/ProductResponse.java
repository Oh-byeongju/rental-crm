package com.rental.backoffice.product.dto;

import com.rental.domain.equipment.entity.Equipment;
import com.rental.domain.product.entity.Product;

import java.time.LocalDateTime;

public record ProductResponse(
        Long productId,
        String productCode,
        Long equipmentId,
        String equipmentCode,
        String equipmentModelName,
        String equipmentManufacturer,
        Integer equipmentStockQty,   // 장비 재고 (그리드 표시용 — 신청 가능 여부 참고)
        String productName,
        Long monthlyFee,
        Integer contractMonths,
        Long depositAmount,
        Long installFee,
        String description,
        String useYn,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {
    public static ProductResponse from(Product p, Equipment e) {
        return new ProductResponse(
                p.getProductId(),
                p.getProductCode(),
                p.getEquipmentId(),
                e != null ? e.getEquipmentCode() : null,
                e != null ? e.getModelName()    : null,
                e != null ? e.getManufacturer() : null,
                e != null ? e.getStockQty()     : null,
                p.getProductName(),
                p.getMonthlyFee(),
                p.getContractMonths(),
                p.getDepositAmount(),
                p.getInstallFee(),
                p.getDescription(),
                p.getUseYn(),
                p.getFirsRegDts(),
                p.getFirsRegUserId(),
                p.getFinaRegDts(),
                p.getFinaRegUserId()
        );
    }
}
