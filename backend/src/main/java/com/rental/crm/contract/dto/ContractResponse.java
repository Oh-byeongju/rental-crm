package com.rental.crm.contract.dto;

import com.rental.crm.contract.entity.Contract;
import com.rental.crm.customer.entity.Customer;
import com.rental.crm.equipment.entity.Equipment;
import com.rental.crm.product.entity.Product;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ContractResponse(
        Long contractId,
        String contractNo,
        Long customerId,
        String customerNo,
        String customerName,
        Long productId,
        String productCode,
        String productName,
        Long equipmentId,
        String equipmentCode,
        String equipmentModelName,
        Long monthlyFee,
        LocalDate startDate,
        LocalDate endDate,
        String installAddress,
        String contractStatus,
        LocalDateTime suspendedAt,
        String suspendReason,
        LocalDateTime resumedAt,
        LocalDateTime terminatedAt,
        String terminateReason,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {
    public static ContractResponse from(Contract c, Customer customer, Product product, Equipment equipment) {
        return new ContractResponse(
                c.getContractId(),
                c.getContractNo(),
                c.getCustomerId(),
                customer  != null ? customer.getCustomerNo()   : null,
                customer  != null ? customer.getCustomerName() : null,
                c.getProductId(),
                product   != null ? product.getProductCode()   : null,
                product   != null ? product.getProductName()   : null,
                equipment != null ? equipment.getEquipmentId() : null,
                equipment != null ? equipment.getEquipmentCode() : null,
                equipment != null ? equipment.getModelName()   : null,
                c.getMonthlyFee(),
                c.getStartDate(),
                c.getEndDate(),
                c.getInstallAddress(),
                c.getContractStatus(),
                c.getSuspendedAt(),
                c.getSuspendReason(),
                c.getResumedAt(),
                c.getTerminatedAt(),
                c.getTerminateReason(),
                c.getFirsRegDts(),
                c.getFirsRegUserId(),
                c.getFinaRegDts(),
                c.getFinaRegUserId()
        );
    }
}
