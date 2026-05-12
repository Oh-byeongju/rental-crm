package com.rental.crm.contract.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 계약 등록 — CONTRACT_NO / MONTHLY_FEE / END_DATE 는 서버 자동 계산.
 * - CONTRACT_NO: `CT-YYYYMMDD-NNNNN` (등록일 + contractId)
 * - MONTHLY_FEE: PRODUCT.MONTHLY_FEE 스냅샷
 * - END_DATE: START_DATE + PRODUCT.CONTRACT_MONTHS
 */
public record ContractCreateRequest(
        @NotNull
        Long customerId,

        @NotNull
        Long productId,

        @NotNull
        LocalDate startDate,

        @NotBlank @Size(max = 500)
        String installAddress
) {}
