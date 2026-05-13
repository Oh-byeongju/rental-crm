package com.rental.backoffice.batch.dto;

/**
 * 배치 트리거 요청 (화면 → backoffice → batch).
 * <p>backoffice/batch 양쪽에 같은 record 가 있지만 모듈 경계 유지 위해 분리.
 */
public record BatchTriggerRequest(
        String billingMonth,   // YYYY-MM (BILLING_CREATE 등)
        Integer roundNo,       // 1~6 (Ch.1)
        String strategy        // single-save / chunk-flush / ...
) {
    public static BatchTriggerRequest empty() {
        return new BatchTriggerRequest(null, null, null);
    }
}
