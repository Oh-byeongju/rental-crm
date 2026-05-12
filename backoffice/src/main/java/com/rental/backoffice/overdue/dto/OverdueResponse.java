package com.rental.backoffice.overdue.dto;

import com.rental.domain.billing.entity.Billing;
import com.rental.domain.customer.entity.Customer;
import com.rental.domain.overdue.entity.Overdue;

import java.time.LocalDateTime;

public record OverdueResponse(
        Long overdueId,
        Long billingId,
        String billingNo,
        String billingMonth,
        Long customerId,
        String customerNo,
        String customerName,
        Long overdueAmount,
        Integer overdueDays,
        Boolean resolved,
        LocalDateTime resolvedAt,
        String resolveReason,
        LocalDateTime createdAt,
        String createdBy
) {
    public static OverdueResponse from(Overdue o, Billing billing, Customer customer) {
        return new OverdueResponse(
                o.getOverdueId(),
                o.getBillingId(),
                billing  != null ? billing.getBillingNo()    : null,
                billing  != null ? billing.getBillingMonth() : null,
                o.getCustomerId(),
                customer != null ? customer.getCustomerNo()   : null,
                customer != null ? customer.getCustomerName() : null,
                o.getOverdueAmount(),
                o.getOverdueDays(),
                o.isResolved(),
                o.getResolvedAt(),
                o.getResolveReason(),
                o.getFirsRegDts(),
                o.getFirsRegUserId()
        );
    }
}
