package com.rental.crm.auth.dto;

import org.springframework.lang.Nullable;

public record RoleSearchRequest(
        @Nullable String roleCode,
        @Nullable String roleName,
        @Nullable String useYn
) {
    public boolean hasRoleCode() { return roleCode != null && !roleCode.isBlank(); }
    public boolean hasRoleName() { return roleName != null && !roleName.isBlank(); }
    public boolean hasUseYn()    { return useYn    != null && !useYn.isBlank(); }
}
