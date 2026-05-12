package com.rental.backoffice.visit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

import java.time.LocalDate;

/**
 * 방문 배정 — VISIT_STATUS 는 SCHEDULED 로 자동.
 */
public record VisitCreateRequest(
        @NotNull
        Long contractId,

        @NotNull
        Long engineerId,

        @NotBlank @Size(max = 20)
        String visitType,

        @NotNull
        LocalDate scheduledDate,

        @Nullable @Size(max = 1000)
        String memo
) {}
