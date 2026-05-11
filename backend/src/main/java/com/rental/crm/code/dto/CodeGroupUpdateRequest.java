package com.rental.crm.code.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CodeGroupUpdateRequest(
        @NotBlank @Size(max = 100)
        String groupName,

        @Size(max = 500)
        String description,

        @NotBlank @Pattern(regexp = "[YN]")
        String useYn
) {}
