package com.rental.domain.report.repository;

import java.time.LocalDate;

/**
 * 미납 현황 리포트 행 — 네이티브 쿼리 인터페이스 프로젝션.
 *
 * <p>Ch.2 학습: 청구 + 고객 + 계약 (+ 연체) 조인 결과. 컬럼 별칭(camelCase)이
 * getter 이름과 매칭되도록 쿼리에서 {@code AS billingNo} 형태로 별칭 부여.
 */
public interface OverdueReportView {

    String getBillingNo();

    String getBillingMonth();

    String getCustomerNo();

    String getCustomerName();

    String getPhone();

    String getContractNo();

    Long getBillingAmount();

    LocalDate getDueDate();

    String getBillingStatus();

    /** BL_OVERDUE LEFT JOIN — UNPAID(연체 미발생) 이면 null. */
    Integer getOverdueDays();
}
