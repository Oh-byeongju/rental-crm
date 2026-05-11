package com.rental.crm.code.dto;

import org.springframework.lang.Nullable;

public record CodeGroupSearchRequest(
        @Nullable String groupCode,
        @Nullable String groupName,
        @Nullable String useYn
) {
    public boolean hasGroupCode() { return groupCode != null && !groupCode.isBlank(); }
    public boolean hasGroupName() { return groupName != null && !groupName.isBlank(); }
    public boolean hasUseYn()     { return useYn     != null && !useYn.isBlank(); }
}
