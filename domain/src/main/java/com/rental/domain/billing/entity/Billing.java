package com.rental.domain.billing.entity;

import com.rental.domain.common.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * 청구 — `BL_BILLING`.
 *
 * <p>정책 (Ch.1 학습 핵심):
 * <ul>
 *   <li>월 청구 일괄 생성 배치 — Service.createMonthly()</li>
 *   <li>멱등성: UNIQUE (CONTRACT_ID, BILLING_MONTH) — 같은 계약·청구월 중복 INSERT 차단</li>
 *   <li>BILLING_NO: `BILL-YYYYMM-NNNNNN` 자동 채번 (등록 후 billingId 받아 UPDATE)</li>
 *   <li>MONTHLY_FEE / 청구액: CT_CONTRACT.MONTHLY_FEE 스냅샷</li>
 *   <li>BATCH_LOG_ID: 어느 배치에서 생성됐는지 추적 (FK BL_BATCH_LOG)</li>
 *   <li>역정규화: CUSTOMER_ID 직접 보유 — 청구 조회 시 CONTRACT JOIN 비용 절감 (ERD §역정규화)</li>
 *   <li>상태 전이: UNPAID → OVERDUE (배치) / UNPAID/OVERDUE → PAID (수납) / UNPAID/OVERDUE → CANCELLED</li>
 * </ul>
 */
@Entity
@Table(name = "BL_BILLING")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Billing extends BaseAuditEntity {

    public static final String STATUS_UNPAID    = "UNPAID";
    public static final String STATUS_OVERDUE   = "OVERDUE";
    public static final String STATUS_PAID      = "PAID";
    public static final String STATUS_CANCELLED = "CANCELLED";

    /** allow-list — 상태 전이 가능 조건. */
    public static final Set<String> PAYABLE_STATUS     = Set.of(STATUS_UNPAID, STATUS_OVERDUE);
    public static final Set<String> CANCELLABLE_STATUS = Set.of(STATUS_UNPAID, STATUS_OVERDUE);
    public static final Set<String> OVERDUE_CANDIDATE  = Set.of(STATUS_UNPAID);

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_bl_billing")
    @SequenceGenerator(name = "seq_bl_billing", sequenceName = "SEQ_BL_BILLING", allocationSize = 50)
    @Column(name = "BILLING_ID")
    private Long billingId;

    @Column(name = "BILLING_NO", length = 50, nullable = false, unique = true)
    private String billingNo;

    @Column(name = "CONTRACT_ID", nullable = false, updatable = false)
    private Long contractId;

    /** 역정규화 — CT_CONTRACT JOIN 회피용 (ERD §7). */
    @Column(name = "CUSTOMER_ID", nullable = false, updatable = false)
    private Long customerId;

    /** 어느 배치 실행에서 생성됐는지 추적. NULL 가능 (수동 등록 시). */
    @Column(name = "BATCH_LOG_ID", updatable = false)
    private Long batchLogId;

    @Column(name = "BILLING_MONTH", length = 7, nullable = false, updatable = false)
    private String billingMonth;

    @Column(name = "BILLING_AMOUNT", nullable = false, updatable = false)
    private Long billingAmount;

    @Column(name = "ISSUE_DATE", nullable = false, updatable = false)
    private LocalDate issueDate;

    @Column(name = "DUE_DATE", nullable = false)
    private LocalDate dueDate;

    @Column(name = "BILLING_STATUS", length = 20, nullable = false)
    private String billingStatus;

    @Column(name = "PAID_AT")
    private LocalDateTime paidAt;

    @Builder
    private Billing(String billingNo, Long contractId, Long customerId, Long batchLogId,
                    String billingMonth, Long billingAmount,
                    LocalDate issueDate, LocalDate dueDate) {
        this.billingNo      = billingNo;
        this.contractId     = contractId;
        this.customerId     = customerId;
        this.batchLogId     = batchLogId;
        this.billingMonth   = billingMonth;
        this.billingAmount  = billingAmount;
        this.issueDate      = issueDate;
        this.dueDate        = dueDate;
        this.billingStatus  = STATUS_UNPAID;
    }

    // ===== 도메인 행위 (Service 가 validate 후 호출) =====

    /** 수납 완료 — UNPAID/OVERDUE → PAID. */
    public void markPaid() {
        this.billingStatus = STATUS_PAID;
        this.paidAt        = LocalDateTime.now();
    }

    /** 연체 처리 — UNPAID → OVERDUE (연체 배치). */
    public void markOverdue() {
        this.billingStatus = STATUS_OVERDUE;
    }

    /** 청구 취소 — UNPAID/OVERDUE → CANCELLED. */
    public void cancel() {
        this.billingStatus = STATUS_CANCELLED;
    }

    /** DUE_DATE 수정 (운영 편의 — 납기 연장 등). */
    public void changeDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    /** BILLING_NO 자동 채번 (등록 후). */
    public void assignBillingNo(String billingNo) { this.billingNo = billingNo; }
}
