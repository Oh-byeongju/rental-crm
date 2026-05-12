package com.rental.backoffice.visit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 방문 취소 — reason 필수 (운영 추적). complete 는 reason 없음 (별도 DTO 없이 빈 body).
 */
public record VisitCancelRequest(
        @NotBlank @Size(max = 1000)
        String reason
) {}
