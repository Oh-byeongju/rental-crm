package com.rental.backoffice.code.dto;

import com.rental.domain.code.entity.Code;

import java.time.LocalDateTime;

public record CodeResponse(
        Long codeId,
        String groupCode,
        String codeValue,
        String codeName,
        Integer sortOrder,
        String description,
        String propVal1,
        String propVal2,
        String propVal3,
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
                c.getDescription(),
                c.getPropVal1(),
                c.getPropVal2(),
                c.getPropVal3(),
                c.getUseYn(),
                c.getFirsRegDts(),
                c.getFirsRegUserId(),
                c.getFinaRegDts(),
                c.getFinaRegUserId()
        );
    }
}
