package com.rental.crm.billing.dto;

import com.rental.crm.billing.entity.BatchLog;

import java.time.LocalDateTime;

public record BatchLogResponse(
        Long batchLogId,
        String batchType,
        String billingMonth,
        String batchStatus,
        Integer targetCount,
        Integer processCount,
        Integer successCount,
        Integer failCount,
        Long durationMs,
        String errorMsg,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        String createdBy
) {
    public static BatchLogResponse from(BatchLog b) {
        return new BatchLogResponse(
                b.getBatchLogId(),
                b.getBatchType(),
                b.getBillingMonth(),
                b.getBatchStatus(),
                b.getTargetCount(),
                b.getProcessCount(),
                b.getSuccessCount(),
                b.getFailCount(),
                b.getDurationMs(),
                b.getErrorMsg(),
                b.getStartedAt(),
                b.getCompletedAt(),
                b.getFirsRegDts(),
                b.getFirsRegUserId()
        );
    }
}
