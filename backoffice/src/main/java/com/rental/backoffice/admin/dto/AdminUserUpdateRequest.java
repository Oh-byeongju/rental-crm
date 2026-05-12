package com.rental.backoffice.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

public record AdminUserUpdateRequest(
        @NotBlank @Size(max = 100)
        String userName,

        @Nullable @Size(max = 20)
        String phone,

        @Nullable
        Long roleId,

        @NotBlank @Pattern(regexp = "[YN]")
        String useYn
) {}
