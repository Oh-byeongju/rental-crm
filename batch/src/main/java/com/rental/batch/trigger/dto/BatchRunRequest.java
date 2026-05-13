package com.rental.batch.trigger.dto;

/**
 * 배치 실행 요청 페이로드 (POST /internal/batch/run/{scenario} body).
 * <p>모든 필드 nullable — 시나리오별로 의미 다름.
 * <ul>
 *   <li>{@code DUMMY_SUCCESS / DUMMY_FAIL} — 모두 NULL OK</li>
 *   <li>{@code BILLING_CREATE} — billingMonth + roundNo + strategy 필수</li>
 * </ul>
 */
public record BatchRunRequest(
        String billingMonth,   // YYYY-MM
        Integer roundNo,       // 1~6
        String strategy        // single-save / chunk-flush / ...
) {
    public static BatchRunRequest empty() {
        return new BatchRunRequest(null, null, null);
    }
}
