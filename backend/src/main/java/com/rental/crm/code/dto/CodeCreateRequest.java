package com.rental.crm.code.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CodeCreateRequest(
        @NotBlank @Size(max = 50)
        String groupCode,

        @NotBlank @Size(max = 50)
        String codeValue,

        @NotBlank @Size(max = 100)
        String codeName,

        Integer sortOrder
) {}
