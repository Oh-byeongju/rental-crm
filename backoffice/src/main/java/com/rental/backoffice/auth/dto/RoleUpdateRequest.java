package com.rental.backoffice.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RoleUpdateRequest(
        @NotBlank @Size(max = 100)
        String roleName,

        @Size(max = 500)
        String description,

        @NotBlank @Pattern(regexp = "[YN]")
        String useYn
) {}
