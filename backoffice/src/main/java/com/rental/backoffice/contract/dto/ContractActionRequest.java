package com.rental.backoffice.contract.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 계약 상태 전이 액션 공통 — suspend / terminate.
 * reason 필수 (운영 추적). resume 은 reason 불필요 (별도 DTO 없이 빈 body).
 */
public record ContractActionRequest(
        @NotBlank @Size(max = 1000)
        String reason
) {}
