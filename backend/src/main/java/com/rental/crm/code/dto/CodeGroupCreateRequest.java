package com.rental.crm.code.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 04 §1-1 — 코드 그룹 키는 영문 대문자 + 언더스코어만.
 */
public record CodeGroupCreateRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "영문 대문자/숫자/언더스코어만 허용 (대문자 시작)")
        @Size(max = 50)
        String groupCode,

        @NotBlank @Size(max = 100)
        String groupName,

        @Size(max = 500)
        String description
) {}
