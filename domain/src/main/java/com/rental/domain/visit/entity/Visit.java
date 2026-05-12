package com.rental.domain.visit.entity;

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
import java.util.Set;

/**
 * 방문 이력 — `CT_VISIT`.
 *
 * <p>정책 (ERD §방문 + 04 §5-1):
 * <ul>
 *   <li>VISIT_TYPE: CM_CODE 그룹 VISIT_TYPE (INSTALL/CHECK/COLLECT) — DB CHECK 제약</li>
 *   <li>VISIT_STATUS: SCHEDULED → COMPLETED / SCHEDULED → CANCELLED (allow-list)</li>
 *   <li>SCHEDULED_DATE 오늘 이후만 (Service 검증)</li>
 *   <li>기사별 같은 날 일정 5건 초과 시 차단 (Service 검증)</li>
 *   <li>CANCEL 시 CANCEL_REASON 필수 (DTO + Service)</li>
 *   <li>USE_YN 없음 — VISIT_STATUS 로 라이프사이클 관리 (이력성 테이블)</li>
 * </ul>
 */
@Entity
@Table(name = "CT_VISIT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Visit extends BaseAuditEntity {

    public static final String STATUS_SCHEDULED = "SCHEDULED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    /** allow-list — 상태 전이 가능 조건. */
    public static final Set<String> COMPLETABLE_STATUS = Set.of(STATUS_SCHEDULED);
    public static final Set<String> CANCELLABLE_STATUS = Set.of(STATUS_SCHEDULED);

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_ct_visit")
    @SequenceGenerator(name = "seq_ct_visit", sequenceName = "SEQ_CT_VISIT", allocationSize = 50)
    @Column(name = "VISIT_ID")
    private Long visitId;

    @Column(name = "CONTRACT_ID", nullable = false, updatable = false)
    private Long contractId;

    @Column(name = "ENGINEER_ID", nullable = false)
    private Long engineerId;

    @Column(name = "VISIT_TYPE", length = 20, nullable = false, updatable = false)
    private String visitType;

    @Column(name = "SCHEDULED_DATE", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "COMPLETED_DATE")
    private LocalDate completedDate;

    @Column(name = "VISIT_STATUS", length = 20, nullable = false)
    private String visitStatus;

    @Column(name = "CANCEL_REASON", length = 1000)
    private String cancelReason;

    @Column(name = "MEMO", length = 1000)
    private String memo;

    @Builder
    private Visit(Long contractId, Long engineerId, String visitType,
                  LocalDate scheduledDate, String memo) {
        this.contractId    = contractId;
        this.engineerId    = engineerId;
        this.visitType     = visitType;
        this.scheduledDate = scheduledDate;
        this.memo          = memo;
        this.visitStatus   = STATUS_SCHEDULED;
    }

    // ===== 도메인 행위 (Service 가 validate 후 호출) =====

    public void complete() {
        this.visitStatus    = STATUS_COMPLETED;
        this.completedDate  = LocalDate.now();
    }

    public void cancel(String reason) {
        this.visitStatus  = STATUS_CANCELLED;
        this.cancelReason = reason;
    }

    // ===== 일반 수정 (SCHEDULED 상태만 — Service 가 검증) =====
    public void changeEngineerId(Long engineerId)        { this.engineerId    = engineerId; }
    public void changeScheduledDate(LocalDate date)      { this.scheduledDate = date; }
    public void changeMemo(String memo)                  { this.memo          = memo; }
}
