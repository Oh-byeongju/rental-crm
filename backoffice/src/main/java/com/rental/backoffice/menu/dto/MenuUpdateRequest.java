package com.rental.backoffice.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MenuUpdateRequest(
        @NotBlank @Size(max = 100)
        String menuName,

        @Size(max = 200)
        String menuUrl,

        @Size(max = 50)
        String iconClass,

        Integer sortOrder,

        @NotBlank @Pattern(regexp = "[YN]")
        String useYn
) {}
