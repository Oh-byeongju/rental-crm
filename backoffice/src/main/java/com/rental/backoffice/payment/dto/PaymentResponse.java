package com.rental.backoffice.payment.dto;

import com.rental.domain.billing.entity.Billing;
import com.rental.domain.customer.entity.Customer;
import com.rental.domain.payment.entity.Payment;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long paymentId,
        String paymentNo,
        Long billingId,
        String billingNo,
        String billingMonth,
        Long customerId,
        String customerNo,
        String customerName,
        Long paymentAmount,
        String paymentMethod,
        LocalDate paymentDate,
        String paymentStatus,
        LocalDateTime cancelledAt,
        String cancelReason,
        String tossOrderId,
        String tossPaymentKey,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {
    public static PaymentResponse from(Payment p, Billing billing, Customer customer) {
        return new PaymentResponse(
                p.getPaymentId(),
                p.getPaymentNo(),
                p.getBillingId(),
                billing  != null ? billing.getBillingNo()    : null,
                billing  != null ? billing.getBillingMonth() : null,
                p.getCustomerId(),
                customer != null ? customer.getCustomerNo()   : null,
                customer != null ? customer.getCustomerName() : null,
                p.getPaymentAmount(),
                p.getPaymentMethod(),
                p.getPaymentDate(),
                p.getPaymentStatus(),
                p.getCancelledAt(),
                p.getCancelReason(),
                p.getTossOrderId(),
                p.getTossPaymentKey(),
                p.getFirsRegDts(),
                p.getFirsRegUserId(),
                p.getFinaRegDts(),
                p.getFinaRegUserId()
        );
    }
}
