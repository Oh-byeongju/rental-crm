package com.rental.backoffice.equipment.dto;

import org.springframework.lang.Nullable;

/**
 * stockFilter: null=전체, "AVAILABLE"=재고 있음(>0), "OUT_OF_STOCK"=재고 없음(=0)
 */
public record EquipmentSearchRequest(
        @Nullable String equipmentCode,
        @Nullable String equipmentType,
        @Nullable String modelName,
        @Nullable String manufacturer,
        @Nullable String useYn,
        @Nullable String stockFilter
) {
    public boolean hasEquipmentCode() { return equipmentCode != null && !equipmentCode.isBlank(); }
    public boolean hasEquipmentType() { return equipmentType != null && !equipmentType.isBlank(); }
    public boolean hasModelName()     { return modelName     != null && !modelName.isBlank(); }
    public boolean hasManufacturer()  { return manufacturer  != null && !manufacturer.isBlank(); }
    public boolean hasUseYn()         { return useYn         != null && !useYn.isBlank(); }
    public boolean hasStockFilter()   { return stockFilter   != null && !stockFilter.isBlank(); }
}
