package com.rental.crm.visit.dto;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;

import java.time.LocalDate;

public record VisitSearchRequest(
        @Nullable Long   contractId,
        @Nullable Long   engineerId,
        @Nullable String visitType,
        @Nullable String visitStatus,
        @Nullable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
        @Nullable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
) {
    public boolean hasContractId()  { return contractId  != null; }
    public boolean hasEngineerId()  { return engineerId  != null; }
    public boolean hasVisitType()   { return visitType   != null && !visitType.isBlank(); }
    public boolean hasVisitStatus() { return visitStatus != null && !visitStatus.isBlank(); }
    public boolean hasDateFrom()    { return dateFrom    != null; }
    public boolean hasDateTo()      { return dateTo      != null; }
}
