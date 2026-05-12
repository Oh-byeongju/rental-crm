package com.rental.crm.contract.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 계약 일반 수정 — END_DATE / INSTALL_ADDRESS 만 수정 가능.
 * 사실관계 (CUSTOMER_ID / PRODUCT_ID / START_DATE / MONTHLY_FEE) 는 불변.
 * 상태 전이는 별도 액션 API (suspend / resume / terminate).
 */
public record ContractUpdateRequest(
        @NotNull
        LocalDate endDate,

        @NotBlank @Size(max = 500)
        String installAddress
) {}
