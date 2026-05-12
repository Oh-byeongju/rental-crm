package com.rental.crm.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

/**
 * 상품 수정 — PRODUCT_CODE 변경 가능 (EQUIPMENT_CODE 와 동일 정책 — UNIQUE 비즈니스 키, DB FK 영향 없음).
 */
public record ProductUpdateRequest(
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
        String description,

        @NotBlank
        @Pattern(regexp = "^[YN]$", message = "Y 또는 N")
        String useYn
) {}
