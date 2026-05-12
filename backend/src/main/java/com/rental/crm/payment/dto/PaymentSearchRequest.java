package com.rental.crm.payment.dto;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;

import java.time.LocalDate;

public record PaymentSearchRequest(
        @Nullable String paymentNo,
        @Nullable Long   billingId,
        @Nullable Long   customerId,
        @Nullable String paymentMethod,
        @Nullable String paymentStatus,
        @Nullable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
        @Nullable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
) {
    public boolean hasPaymentNo()     { return paymentNo     != null && !paymentNo.isBlank(); }
    public boolean hasBillingId()     { return billingId     != null; }
    public boolean hasCustomerId()    { return customerId    != null; }
    public boolean hasPaymentMethod() { return paymentMethod != null && !paymentMethod.isBlank(); }
    public boolean hasPaymentStatus() { return paymentStatus != null && !paymentStatus.isBlank(); }
    public boolean hasDateFrom()      { return dateFrom      != null; }
    public boolean hasDateTo()        { return dateTo        != null; }
}
