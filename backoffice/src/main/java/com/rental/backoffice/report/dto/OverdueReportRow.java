package com.rental.backoffice.report.dto;

import com.rental.domain.report.repository.OverdueReportView;

import java.time.LocalDate;

/**
 * 미납 현황 리포트 행 — 화면 JSON 응답 + 엑셀 행의 공통 표현.
 */
public record OverdueReportRow(
        String billingNo,
        String billingMonth,
        String customerNo,
        String customerName,
        String phone,
        String contractNo,
        Long billingAmount,
        LocalDate dueDate,
        String billingStatus,
        Integer overdueDays
) {
    public static OverdueReportRow from(OverdueReportView v) {
        return new OverdueReportRow(
                v.getBillingNo(),
                v.getBillingMonth(),
                v.getCustomerNo(),
                v.getCustomerName(),
                v.getPhone(),
                v.getContractNo(),
                v.getBillingAmount(),
                v.getDueDate(),
                v.getBillingStatus(),
                v.getOverdueDays());
    }
}
