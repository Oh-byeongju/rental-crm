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

import java.time.LocalDateTime;

/**
 * 배치 실행 이력 — `BL_BATCH_LOG`. Ch.1 학습 핵심 — 멱등성 + 성능 측정.
 *
 * <p>정책 (Ch.1):
 * <ul>
 *   <li>BATCH_TYPE: BILLING_CREATE (월 청구 일괄 생성) / OVERDUE_UPDATE (연체 배치)</li>
 *   <li>멱등성: UNIQUE (BATCH_TYPE, BILLING_MONTH) — 동일 청구월 중복 실행 차단</li>
 *   <li>DURATION_MS: 성능 측정용 (Ch.1 — 건별 INSERT vs JDBC bulk INSERT 비교)</li>
 *   <li>BATCH_STATUS: RUNNING → COMPLETED / FAILED</li>
 * </ul>
 */
@Entity
@Table(name = "BL_BATCH_LOG")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BatchLog extends BaseAuditEntity {

    public static final String TYPE_BILLING_CREATE = "BILLING_CREATE";
    public static final String TYPE_OVERDUE_UPDATE = "OVERDUE_UPDATE";
    /** ADR-014 Step 5 — 통신 뼈대 검증용 더미. Step 7 실 시나리오 도입 시 제거 후보. */
    public static final String TYPE_DUMMY_SUCCESS  = "DUMMY_SUCCESS";
    public static final String TYPE_DUMMY_FAIL     = "DUMMY_FAIL";

    public static final String STATUS_RUNNING   = "RUNNING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED    = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_bl_batch_log")
    @SequenceGenerator(name = "seq_bl_batch_log", sequenceName = "SEQ_BL_BATCH_LOG", allocationSize = 50)
    @Column(name = "BATCH_LOG_ID")
    private Long batchLogId;

    @Column(name = "BATCH_TYPE", length = 50, nullable = false, updatable = false)
    private String batchType;

    /** YYYY-MM (BILLING_CREATE 시 사용. OVERDUE_UPDATE 는 NULL 가능). */
    @Column(name = "BILLING_MONTH", length = 7, updatable = false)
    private String billingMonth;

    @Column(name = "BATCH_STATUS", length = 20, nullable = false)
    private String batchStatus;

    @Column(name = "TARGET_COUNT", nullable = false)
    private Integer targetCount;

    @Column(name = "PROCESS_COUNT", nullable = false)
    private Integer processCount;

    @Column(name = "SUCCESS_COUNT", nullable = false)
    private Integer successCount;

    @Column(name = "FAIL_COUNT", nullable = false)
    private Integer failCount;

    /** 실행 시간 (밀리초) — Ch.1 성능 측정 핵심 컬럼. */
    @Column(name = "DURATION_MS")
    private Long durationMs;

    @Column(name = "ERROR_MSG", length = 2000)
    private String errorMsg;

    @Column(name = "STARTED_AT", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "COMPLETED_AT")
    private LocalDateTime completedAt;

    @Builder
    private BatchLog(String batchType, String billingMonth, Integer targetCount) {
        this.batchType    = batchType;
        this.billingMonth = billingMonth;
        this.batchStatus  = STATUS_RUNNING;
        this.targetCount  = targetCount == null ? 0 : targetCount;
        this.processCount = 0;
        this.successCount = 0;
        this.failCount    = 0;
        this.startedAt    = LocalDateTime.now();
    }

    // ===== 도메인 행위 =====

    public void addProcess(boolean success) {
        this.processCount++;
        if (success) this.successCount++;
        else         this.failCount++;
    }

    /** 배치 완료 처리 — 실행 시간 계산. */
    public void markCompleted() {
        this.batchStatus  = STATUS_COMPLETED;
        this.completedAt  = LocalDateTime.now();
        this.durationMs   = java.time.Duration.between(this.startedAt, this.completedAt).toMillis();
    }

    public void markFailed(String errorMsg) {
        this.batchStatus  = STATUS_FAILED;
        this.completedAt  = LocalDateTime.now();
        this.durationMs   = java.time.Duration.between(this.startedAt, this.completedAt).toMillis();
        this.errorMsg     = errorMsg;
    }
}
