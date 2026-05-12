package com.rental.backoffice.equipment.dto;

import com.rental.domain.equipment.entity.Equipment;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EquipmentResponse(
        Long equipmentId,
        String equipmentCode,
        String equipmentType,
        String equipmentTypeName,   // CM_CODE 그룹 EQUIPMENT_TYPE 의 CODE_NAME
        String modelName,
        String manufacturer,
        LocalDate releaseDate,
        String imageUrl,
        String description,
        Integer stockQty,
        String useYn,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {
    public static EquipmentResponse from(Equipment e, String equipmentTypeName) {
        return new EquipmentResponse(
                e.getEquipmentId(),
                e.getEquipmentCode(),
                e.getEquipmentType(),
                equipmentTypeName,
                e.getModelName(),
                e.getManufacturer(),
                e.getReleaseDate(),
                e.getImageUrl(),
                e.getDescription(),
                e.getStockQty(),
                e.getUseYn(),
                e.getFirsRegDts(),
                e.getFirsRegUserId(),
                e.getFinaRegDts(),
                e.getFinaRegUserId()
        );
    }
}
