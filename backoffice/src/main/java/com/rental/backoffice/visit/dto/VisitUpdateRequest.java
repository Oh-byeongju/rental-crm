package com.rental.backoffice.visit.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

import java.time.LocalDate;

/**
 * 방문 일정 수정 — SCHEDULED 상태만 가능 (Service 검증).
 * 변경 가능: engineerId (재배정), scheduledDate, memo.
 * VISIT_TYPE / CONTRACT_ID 불변.
 */
public record VisitUpdateRequest(
        @NotNull
        Long engineerId,

        @NotNull
        LocalDate scheduledDate,

        @Nullable @Size(max = 1000)
        String memo
) {}
