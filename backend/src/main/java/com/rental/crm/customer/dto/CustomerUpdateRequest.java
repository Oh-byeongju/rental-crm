package com.rental.crm.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 고객 수정 — 이메일/비밀번호 변경 X (별도 엔드포인트).
 * useYn 으로 활성/비활성 일괄 전환 (모달 select 로 통합).
 */
public record CustomerUpdateRequest(

        @NotBlank
        @Size(max = 100)
        String customerName,

        @NotBlank
        @Pattern(regexp = "^[0-9]{10,11}$")
        String phone,

        LocalDate birthDate,

        @Size(max = 10)
        String addressZip,

        @NotBlank
        @Size(max = 500)
        String address,

        @NotBlank
        @Pattern(regexp = "^[YN]$", message = "사용여부 Y/N")
        String useYn,

        @Size(max = 100)
        String wrkRmk
) {}
