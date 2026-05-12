package com.rental.crm.equipment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

import java.time.LocalDate;

public record EquipmentCreateRequest(
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
        Integer stockQty
) {}
