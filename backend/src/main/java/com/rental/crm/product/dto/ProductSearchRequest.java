package com.rental.crm.product.dto;

import org.springframework.lang.Nullable;

public record ProductSearchRequest(
        @Nullable String productCode,
        @Nullable Long   equipmentId,
        @Nullable String productName,
        @Nullable String useYn
) {
    public boolean hasProductCode() { return productCode != null && !productCode.isBlank(); }
    public boolean hasEquipmentId() { return equipmentId != null; }
    public boolean hasProductName() { return productName != null && !productName.isBlank(); }
    public boolean hasUseYn()       { return useYn       != null && !useYn.isBlank(); }
}
