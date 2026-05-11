package com.rental.crm.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CustomerCreateRequest(

        @NotBlank(message = "고객명 필수")
        @Size(max = 100)
        String customerName,

        @NotBlank @Email(message = "이메일 형식")
        @Size(max = 254)
        String email,

        @NotBlank
        @Size(min = 8, max = 64, message = "비밀번호 8~64자")
        String password,

        @NotBlank
        @Pattern(regexp = "^[0-9]{10,11}$", message = "연락처는 숫자 10~11자리")
        String phone,

        LocalDate birthDate,

        @Size(max = 10)
        String addressZip,

        @NotBlank
        @Size(max = 500)
        String address,

        @NotBlank
        @Pattern(regexp = "^[YN]$", message = "약관 동의 Y/N")
        String termsAgreeYn,

        @Size(max = 100)
        String wrkRmk
) {}
