package com.rental.crm.code.dto;

import com.rental.crm.code.entity.Code;

import java.time.LocalDateTime;

public record CodeResponse(
        Long codeId,
        String groupCode,
        String codeValue,
        String codeName,
        Integer sortOrder,
        String useYn,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {
    public static CodeResponse from(Code c) {
        return new CodeResponse(
                c.getCodeId(),
                c.getGroupCode(),
                c.getCodeValue(),
                c.getCodeName(),
                c.getSortOrder(),
                c.getUseYn(),
                c.getFirsRegDts(),
                c.getFirsRegUserId(),
                c.getFinaRegDts(),
                c.getFinaRegUserId()
        );
    }
}
