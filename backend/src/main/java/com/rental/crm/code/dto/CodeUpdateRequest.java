package com.rental.crm.code.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CodeUpdateRequest(
        @NotBlank @Size(max = 100)
        String codeName,

        Integer sortOrder,

        @NotBlank @Pattern(regexp = "[YN]")
        String useYn
) {}
