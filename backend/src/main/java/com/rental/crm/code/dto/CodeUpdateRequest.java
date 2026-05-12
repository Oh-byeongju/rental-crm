package com.rental.crm.code.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

public record CodeUpdateRequest(
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
        String propVal3,

        @NotBlank @Pattern(regexp = "[YN]")
        String useYn
) {}
