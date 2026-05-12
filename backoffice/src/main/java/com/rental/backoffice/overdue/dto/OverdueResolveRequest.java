package com.rental.backoffice.overdue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 연체 수동 해제 — reason 필수 (운영 추적).
 */
public record OverdueResolveRequest(
        @NotBlank @Size(max = 1000)
        String reason
) {}
