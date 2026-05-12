package com.rental.backoffice.contract.dto;

import org.springframework.lang.Nullable;

public record ContractSearchRequest(
        @Nullable String contractNo,
        @Nullable Long   customerId,
        @Nullable Long   productId,
        @Nullable String contractStatus
) {
    public boolean hasContractNo()     { return contractNo     != null && !contractNo.isBlank(); }
    public boolean hasCustomerId()     { return customerId     != null; }
    public boolean hasProductId()      { return productId      != null; }
    public boolean hasContractStatus() { return contractStatus != null && !contractStatus.isBlank(); }
}
