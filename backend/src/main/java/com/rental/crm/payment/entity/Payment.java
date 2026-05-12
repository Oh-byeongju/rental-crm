package com.rental.crm.payment.entity;

import com.rental.crm.common.entity.BaseAuditEntity;
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
 * 수납 — `BL_PAYMENT`.
 *
 * <p>정책 (Ch.3 학습 핵심 — Kafka Producer/Consumer):
 * <ul>
 *   <li>PAYMENT_NO: `PAY-YYYYMM-NNNNNN` 자동 채번 (등록 후 paymentId 받아 UPDATE)</li>
 *   <li>BILLING_ID: 청구 FK. 청구 PAYABLE_STATUS (UNPAID/OVERDUE) 만 수납 등록 가능</li>
 *   <li>분할 수납 X — 청구 금액과 수납 금액 일치만 허용 (학습 단순화)</li>
 *   <li>PAYMENT_METHOD: CM_CODE 그룹 PAYMENT_METHOD (CARD / BANK / CASH / TOSS)</li>
 *   <li>Toss Payments 연동 컬럼 — TOSS_ORDER_ID (UNIQUE, NULL 가능) + TOSS_PAYMENT_KEY. METHOD='TOSS' 일 때만</li>
 *   <li>상태 전이: COMPLETED → CANCELLED / COMPLETED → REFUNDED. 환불은 Toss API 호출 후 트리거</li>
 *   <li>Kafka 토픽 발행: 등록 시 `rental.payment.completed`, 취소 시 `rental.payment.cancelled` (Ch.3 작업 시 추가)</li>
 * </ul>
 */
@Entity
@Table(name = "BL_PAYMENT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseAuditEntity {

    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_REFUNDED  = "REFUNDED";

    public static final String METHOD_CARD = "CARD";
    public static final String METHOD_BANK = "BANK";
    public static final String METHOD_CASH = "CASH";
    public static final String METHOD_TOSS = "TOSS";

    /** allow-list — 상태 전이 가능 조건. */
    public static final Set<String> CANCELLABLE_STATUS = Set.of(STATUS_COMPLETED);
    public static final Set<String> REFUNDABLE_STATUS  = Set.of(STATUS_COMPLETED);

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_bl_payment")
    @SequenceGenerator(name = "seq_bl_payment", sequenceName = "SEQ_BL_PAYMENT", allocationSize = 50)
    @Column(name = "PAYMENT_ID")
    private Long paymentId;

    @Column(name = "PAYMENT_NO", length = 50, nullable = false, unique = true)
    private String paymentNo;

    @Column(name = "BILLING_ID", nullable = false, updatable = false)
    private Long billingId;

    @Column(name = "CUSTOMER_ID", nullable = false, updatable = false)
    private Long customerId;

    @Column(name = "PAYMENT_AMOUNT", nullable = false, updatable = false)
    private Long paymentAmount;

    @Column(name = "PAYMENT_METHOD", length = 20, nullable = false, updatable = false)
    private String paymentMethod;

    @Column(name = "PAYMENT_DATE", nullable = false, updatable = false)
    private LocalDate paymentDate;

    @Column(name = "PAYMENT_STATUS", length = 20, nullable = false)
    private String paymentStatus;

    @Column(name = "CANCELLED_AT")
    private LocalDateTime cancelledAt;

    @Column(name = "CANCEL_REASON", length = 1000)
    private String cancelReason;

    /** Toss Payments 주문번호. METHOD='TOSS' 일 때만. UNIQUE (NULL 허용). */
    @Column(name = "TOSS_ORDER_ID", length = 200, updatable = false)
    private String tossOrderId;

    @Column(name = "TOSS_PAYMENT_KEY", length = 200, updatable = false)
    private String tossPaymentKey;

    @Builder
    private Payment(String paymentNo, Long billingId, Long customerId,
                    Long paymentAmount, String paymentMethod, LocalDate paymentDate,
                    String tossOrderId, String tossPaymentKey) {
        this.paymentNo       = paymentNo;
        this.billingId       = billingId;
        this.customerId      = customerId;
        this.paymentAmount   = paymentAmount;
        this.paymentMethod   = paymentMethod;
        this.paymentDate     = paymentDate;
        this.tossOrderId     = tossOrderId;
        this.tossPaymentKey  = tossPaymentKey;
        this.paymentStatus   = STATUS_COMPLETED;
    }

    // ===== 도메인 행위 (Service 가 validate 후 호출) =====

    public void cancel(String reason) {
        this.paymentStatus = STATUS_CANCELLED;
        this.cancelledAt   = LocalDateTime.now();
        this.cancelReason  = reason;
    }

    public void refund(String reason) {
        this.paymentStatus = STATUS_REFUNDED;
        this.cancelledAt   = LocalDateTime.now();
        this.cancelReason  = reason;
    }

    public void assignPaymentNo(String paymentNo) { this.paymentNo = paymentNo; }
}
