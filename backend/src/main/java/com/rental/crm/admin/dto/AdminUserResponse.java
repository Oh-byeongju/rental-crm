package com.rental.crm.admin.dto;

import com.rental.crm.admin.entity.AdminUser;
import com.rental.crm.auth.entity.Role;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long userId,
        String email,
        String userName,
        String phone,
        Long roleId,
        String roleCode,
        String roleName,
        String useYn,
        boolean locked,
        Integer loginFailCnt,
        LocalDateTime lockedAt,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {
    public static AdminUserResponse from(AdminUser u, Role role) {
        return new AdminUserResponse(
                u.getUserId(),
                u.getEmail(),
                u.getUserName(),
                u.getPhone(),
                u.getRoleId(),
                role != null ? role.getRoleCode() : null,
                role != null ? role.getRoleName() : null,
                u.getUseYn(),
                u.isLocked(),
                u.getLoginFailCnt(),
                u.getLockedAt(),
                u.getLastLoginAt(),
                u.getFirsRegDts(),
                u.getFirsRegUserId(),
                u.getFinaRegDts(),
                u.getFinaRegUserId()
        );
    }
}
