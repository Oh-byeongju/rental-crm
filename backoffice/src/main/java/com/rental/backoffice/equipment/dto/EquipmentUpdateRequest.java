package com.rental.backoffice.equipment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

import java.time.LocalDate;

/**
 * 장비 수정 — EQUIPMENT_CODE 변경 가능 (UNIQUE 비즈니스 키, DB FK 영향 없음).
 */
public record EquipmentUpdateRequest(
        @NotBlank @Size(max = 20)
        String equipmentCode,

        @NotBlank @Size(max = 50)
        String equipmentType,

        @NotBlank @Size(max = 200)
        String modelName,

        @NotBlank @Size(max = 200)
        String manufacturer,

        @Nullable
        LocalDate releaseDate,

        @Nullable @Size(max = 500)
        String imageUrl,

        @Nullable @Size(max = 1000)
        String description,

        @NotNull @Min(0)
        Integer stockQty,

        @NotBlank
        @Pattern(regexp = "^[YN]$", message = "Y 또는 N")
        String useYn
) {}
