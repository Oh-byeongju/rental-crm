package com.rental.backoffice.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

public record MenuCreateRequest(
        /** null = 루트 (depth 1). non-null = 해당 GROUP 의 자식 (depth 2). */
        @Nullable Long parentMenuId,

        @NotBlank @Size(max = 100)
        String menuName,

        @NotBlank @Pattern(regexp = "GROUP|LEAF", message = "GROUP 또는 LEAF")
        String menuType,

        /** LEAF 일 때만 사용. GROUP 은 null. */
        @Size(max = 200)
        String menuUrl,

        @Size(max = 50)
        String iconClass,

        Integer sortOrder
) {}
