package com.rental.backoffice.engineer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

public record EngineerCreateRequest(
        @NotBlank @Size(max = 20)
        String engineerCode,

        @NotBlank @Size(max = 100)
        String engineerName,

        @NotBlank
        @Pattern(regexp = "^(INTERNAL|EXTERNAL)$", message = "INTERNAL 또는 EXTERNAL")
        String engineerType,

        @NotBlank @Size(max = 20)
        String phone,

        @Nullable @Email @Size(max = 254)
        String email,

        @Nullable @Size(max = 100)
        String area
) {}
