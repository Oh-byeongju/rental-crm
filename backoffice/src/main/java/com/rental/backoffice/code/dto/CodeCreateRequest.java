package com.rental.backoffice.code.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

public record CodeCreateRequest(
        @NotBlank @Size(max = 50)
        String groupCode,

        @NotBlank @Size(max = 50)
        String codeValue,

        @NotBlank @Size(max = 100)
        String codeName,

        Integer sortOrder,

        @Nullable @Size(max = 200)
        String description,

        @Nullable @Size(max = 100)
        String propVal1,

        @Nullable @Size(max = 100)
        String propVal2,

        @Nullable @Size(max = 100)
        String propVal3
) {}
