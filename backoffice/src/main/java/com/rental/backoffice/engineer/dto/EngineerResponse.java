package com.rental.backoffice.engineer.dto;

import com.rental.domain.engineer.entity.Engineer;

import java.time.LocalDateTime;

public record EngineerResponse(
        Long engineerId,
        String engineerCode,
        String engineerName,
        String engineerType,
        String phone,
        String email,
        String area,
        String useYn,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {
    public static EngineerResponse from(Engineer e) {
        return new EngineerResponse(
                e.getEngineerId(),
                e.getEngineerCode(),
                e.getEngineerName(),
                e.getEngineerType(),
                e.getPhone(),
                e.getEmail(),
                e.getArea(),
                e.getUseYn(),
                e.getFirsRegDts(),
                e.getFirsRegUserId(),
                e.getFinaRegDts(),
                e.getFinaRegUserId()
        );
    }
}
