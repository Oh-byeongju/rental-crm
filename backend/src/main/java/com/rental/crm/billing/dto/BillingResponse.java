package com.rental.crm.billing.dto;

import com.rental.crm.billing.entity.Billing;
import com.rental.crm.contract.entity.Contract;
import com.rental.crm.customer.entity.Customer;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BillingResponse(
        Long billingId,
        String billingNo,
        Long contractId,
        String contractNo,
        Long customerId,
        String customerNo,
        String customerName,
        Long batchLogId,
        String billingMonth,
        Long billingAmount,
        LocalDate issueDate,
        LocalDate dueDate,
        String billingStatus,
        LocalDateTime paidAt,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {
    public static BillingResponse from(Billing b, Contract contract, Customer customer) {
        return new BillingResponse(
                b.getBillingId(),
                b.getBillingNo(),
                b.getContractId(),
                contract != null ? contract.getContractNo() : null,
                b.getCustomerId(),
                customer != null ? customer.getCustomerNo()   : null,
                customer != null ? customer.getCustomerName() : null,
                b.getBatchLogId(),
                b.getBillingMonth(),
                b.getBillingAmount(),
                b.getIssueDate(),
                b.getDueDate(),
                b.getBillingStatus(),
                b.getPaidAt(),
                b.getFirsRegDts(),
                b.getFirsRegUserId(),
                b.getFinaRegDts(),
                b.getFinaRegUserId()
        );
    }
}
