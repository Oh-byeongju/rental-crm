package com.rental.crm.auth.dto;

import com.rental.crm.auth.entity.Role;

import java.time.LocalDateTime;

public record RoleResponse(
        Long roleId,
        String roleCode,
        String roleName,
        String description,
        String useYn,
        boolean superAdmin,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {
    public static RoleResponse from(Role r) {
        return new RoleResponse(
                r.getRoleId(),
                r.getRoleCode(),
                r.getRoleName(),
                r.getDescription(),
                r.getUseYn(),
                r.isSuperAdmin(),
                r.getFirsRegDts(),
                r.getFirsRegUserId(),
                r.getFinaRegDts(),
                r.getFinaRegUserId()
        );
    }
}
