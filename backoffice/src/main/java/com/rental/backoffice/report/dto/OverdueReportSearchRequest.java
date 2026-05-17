package com.rental.backoffice.report.dto;

import jakarta.annotation.Nullable;

/**
 * 미납 현황 리포트 검색 조건 (화면 조회 / 엑셀 공통).
 *
 * <ul>
 *   <li>{@code billingMonth} — YYYY-MM (null = 전체 월)</li>
 *   <li>{@code status} — UNPAID / OVERDUE (null = 미수납 전체)</li>
 *   <li>{@code customerName} — 부분 일치 LIKE (null = 전체)</li>
 * </ul>
 */
public record OverdueReportSearchRequest(
        @Nullable String billingMonth,
        @Nullable String status,
        @Nullable String customerName
) {
    private static String trimToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    /** 빈 문자열 → null 정규화 (네이티브 쿼리의 :param IS NULL 분기용). */
    public OverdueReportSearchRequest normalized() {
        return new OverdueReportSearchRequest(
                trimToNull(billingMonth),
                trimToNull(status),
                trimToNull(customerName));
    }
}
