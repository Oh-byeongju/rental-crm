package com.rental.backoffice.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

public record ProductCreateRequest(
        @NotBlank @Size(max = 20)
        String productCode,

        @NotNull
        Long equipmentId,

        @NotBlank @Size(max = 200)
        String productName,

        @NotNull @Positive
        Long monthlyFee,

        @NotNull @Min(1)
        Integer contractMonths,

        @NotNull @Min(0)
        Long depositAmount,

        @NotNull @Min(0)
        Long installFee,

        @Nullable @Size(max = 1000)
        String description
) {}
