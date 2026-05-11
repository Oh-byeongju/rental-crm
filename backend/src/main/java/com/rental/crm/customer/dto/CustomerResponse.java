package com.rental.crm.customer.dto;

import com.rental.crm.customer.entity.Customer;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CustomerResponse(
        Long customerId,
        String customerNo,
        String customerName,
        String email,
        String phone,
        LocalDate birthDate,
        String addressZip,
        String address,
        String termsAgreeYn,
        String useYn,
        Integer loginFailCnt,
        LocalDateTime lockedAt,
        LocalDateTime lastLoginAt,
        String wrkRmk,
        LocalDateTime firsRegDts,
        LocalDateTime finaRegDts
) {
    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(
                c.getCustomerId(),
                c.getCustomerNo(),
                c.getCustomerName(),
                c.getEmail(),
                c.getPhone(),
                c.getBirthDate(),
                c.getAddressZip(),
                c.getAddress(),
                c.getTermsAgreeYn(),
                c.getUseYn(),
                c.getLoginFailCnt(),
                c.getLockedAt(),
                c.getLastLoginAt(),
                c.getWrkRmk(),
                c.getFirsRegDts(),
                c.getFinaRegDts()
        );
    }
}
