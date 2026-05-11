package com.rental.crm.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RoleCreateRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "영문 대문자/숫자/언더스코어 (대문자 시작)")
        @Size(max = 50)
        String roleCode,

        @NotBlank @Size(max = 100)
        String roleName,

        @Size(max = 500)
        String description
) {}
