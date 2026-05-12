package com.rental.crm.overdue.dto;

import org.springframework.lang.Nullable;

/**
 * resolvedOnly: null=전체 / "Y"=해결 / "N"=미해결
 */
public record OverdueSearchRequest(
        @Nullable Long    customerId,
        @Nullable Long    billingId,
        @Nullable String  resolvedOnly,
        @Nullable Integer minDays
) {
    public boolean hasCustomerId()   { return customerId   != null; }
    public boolean hasBillingId()    { return billingId    != null; }
    public boolean hasResolvedOnly() { return resolvedOnly != null && !resolvedOnly.isBlank(); }
    public boolean hasMinDays()      { return minDays      != null; }
}
