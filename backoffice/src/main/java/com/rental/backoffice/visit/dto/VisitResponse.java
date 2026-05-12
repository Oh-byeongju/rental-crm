package com.rental.backoffice.visit.dto;

import com.rental.domain.contract.entity.Contract;
import com.rental.domain.engineer.entity.Engineer;
import com.rental.domain.visit.entity.Visit;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record VisitResponse(
        Long visitId,
        Long contractId,
        String contractNo,
        Long engineerId,
        String engineerCode,
        String engineerName,
        String engineerArea,
        String visitType,
        LocalDate scheduledDate,
        LocalDate completedDate,
        String visitStatus,
        String cancelReason,
        String memo,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {
    public static VisitResponse from(Visit v, Contract contract, Engineer engineer) {
        return new VisitResponse(
                v.getVisitId(),
                v.getContractId(),
                contract != null ? contract.getContractNo() : null,
                v.getEngineerId(),
                engineer != null ? engineer.getEngineerCode() : null,
                engineer != null ? engineer.getEngineerName() : null,
                engineer != null ? engineer.getArea()         : null,
                v.getVisitType(),
                v.getScheduledDate(),
                v.getCompletedDate(),
                v.getVisitStatus(),
                v.getCancelReason(),
                v.getMemo(),
                v.getFirsRegDts(),
                v.getFirsRegUserId(),
                v.getFinaRegDts(),
                v.getFinaRegUserId()
        );
    }
}
