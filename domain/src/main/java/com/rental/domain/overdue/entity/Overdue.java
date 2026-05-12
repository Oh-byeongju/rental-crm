package com.rental.domain.overdue.entity;

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

import java.time.LocalDateTime;

/**
 * 연체 — `BL_OVERDUE`.
 *
 * <p>정책:
 * <ul>
 *   <li>UNIQUE (BILLING_ID): 한 청구당 한 연체 레코드 (재발생 시 RESOLVED_AT 갱신, 새 레코드 X)</li>
 *   <li>RESOLVED_AT IS NULL → 미해결. NOT NULL → 해결됨</li>
 *   <li>OVERDUE_DAYS: 배치 실행 시점 기준 (Ch.3 OVERDUE_UPDATE 배치가 갱신)</li>
 *   <li>OVERDUE_AMOUNT: 청구금액 스냅샷</li>
 *   <li>해제 트리거: 수납 완료 / 수동 해제 / 계약 해지 시</li>
 * </ul>
 */
@Entity
@Table(name = "BL_OVERDUE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Overdue extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_bl_overdue")
    @SequenceGenerator(name = "seq_bl_overdue", sequenceName = "SEQ_BL_OVERDUE", allocationSize = 50)
    @Column(name = "OVERDUE_ID")
    private Long overdueId;

    @Column(name = "BILLING_ID", nullable = false, updatable = false, unique = true)
    private Long billingId;

    @Column(name = "CUSTOMER_ID", nullable = false, updatable = false)
    private Long customerId;

    @Column(name = "OVERDUE_AMOUNT", nullable = false, updatable = false)
    private Long overdueAmount;

    @Column(name = "OVERDUE_DAYS", nullable = false)
    private Integer overdueDays;

    @Column(name = "RESOLVED_AT")
    private LocalDateTime resolvedAt;

    @Column(name = "RESOLVE_REASON", length = 1000)
    private String resolveReason;

    @Builder
    private Overdue(Long billingId, Long customerId, Long overdueAmount, Integer overdueDays) {
        this.billingId      = billingId;
        this.customerId     = customerId;
        this.overdueAmount  = overdueAmount;
        this.overdueDays    = overdueDays == null ? 0 : overdueDays;
    }

    // ===== 도메인 행위 =====

    /** 연체 해제 — 수납 완료 / 수동 해제 / 계약 해지 트리거. */
    public void resolve(String reason) {
        this.resolvedAt    = LocalDateTime.now();
        this.resolveReason = reason;
    }

    /** 배치가 연체일수 갱신 (Ch.3 OVERDUE_UPDATE). 이미 해결된 건은 갱신 안 함. */
    public void updateDays(int days) {
        if (this.resolvedAt != null) return;
        this.overdueDays = days;
    }

    /** 재발생 — 한 청구가 다시 연체 (수납 취소 등) 시. 기존 해제 정보 초기화. */
    public void reopen(int days) {
        this.resolvedAt    = null;
        this.resolveReason = null;
        this.overdueDays   = days;
    }

    public boolean isResolved() {
        return this.resolvedAt != null;
    }
}
