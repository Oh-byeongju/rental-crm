package com.rental.crm.code.dto;

import com.rental.crm.code.entity.CodeGroup;

import java.time.LocalDateTime;

public record CodeGroupResponse(
        String groupCode,
        String groupName,
        String description,
        String systemYn,
        String useYn,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {
    public static CodeGroupResponse from(CodeGroup g) {
        return new CodeGroupResponse(
                g.getGroupCode(),
                g.getGroupName(),
                g.getDescription(),
                g.getSystemYn(),
                g.getUseYn(),
                g.getFirsRegDts(),
                g.getFirsRegUserId(),
                g.getFinaRegDts(),
                g.getFinaRegUserId()
        );
    }
}
