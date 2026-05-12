package com.rental.crm.billing.dto;

import org.springframework.lang.Nullable;

public record BillingSearchRequest(
        @Nullable String billingNo,
        @Nullable Long   customerId,
        @Nullable Long   contractId,
        @Nullable String billingMonth,
        @Nullable String billingStatus
) {
    public boolean hasBillingNo()     { return billingNo     != null && !billingNo.isBlank(); }
    public boolean hasCustomerId()    { return customerId    != null; }
    public boolean hasContractId()    { return contractId    != null; }
    public boolean hasBillingMonth()  { return billingMonth  != null && !billingMonth.isBlank(); }
    public boolean hasBillingStatus() { return billingStatus != null && !billingStatus.isBlank(); }
}
