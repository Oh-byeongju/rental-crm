package com.rental.backoffice.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

/**
 * 04 §1-2: 이메일 형식 + 비밀번호 8자+영문/숫자/특수문자.
 */
public record AdminUserCreateRequest(
        @NotBlank @Email @Size(max = 254)
        String email,

        @NotBlank
        @Size(min = 8, max = 64)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                 message = "영문+숫자+특수문자 조합")
        String password,

        @NotBlank @Size(max = 100)
        String userName,

        @Nullable @Size(max = 20)
        String phone,

        /** null = 역할 미부여 (생성 직후엔 권한 없음). */
        @Nullable
        Long roleId
) {}
