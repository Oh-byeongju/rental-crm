package com.rental.backoffice.billing.dto;

import org.springframework.lang.Nullable;

public record BatchLogSearchRequest(
        @Nullable String batchType,
        @Nullable String batchStatus
) {
    public boolean hasBatchType()   { return batchType   != null && !batchType.isBlank(); }
    public boolean hasBatchStatus() { return batchStatus != null && !batchStatus.isBlank(); }
}
