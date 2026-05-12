package com.rental.crm.contract.entity;

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
 * 렌탈 계약 — `CT_CONTRACT`.
 *
 * <p>정책 (D1~D10):
 * <ul>
 *   <li>CONTRACT_NO: 등록 시 서버 자동 채번 — `CT-YYYYMMDD-NNNNN`</li>
 *   <li>상태 전이 allow-list — `backend/guide/conventions/delete-defense.md` 본격 적용 첫 사례:
 *       ACTIVE → SUSPENDED / SUSPENDED → ACTIVE / ACTIVE → TERMINATED / SUSPENDED → TERMINATED.
 *       TERMINATED 는 최종 상태 (변경 불가)</li>
 *   <li>EXECUTE 권한: CONTRACT_SUSPEND / RESUME / TERMINATE 분리 (시드 완비)</li>
 *   <li>MONTHLY_FEE: 등록 시 PRODUCT.MONTHLY_FEE 자동 복사 (스냅샷 — 상품가 변경 영향 없음, updatable=false)</li>
 *   <li>END_DATE: 등록 시 START_DATE + PRODUCT.CONTRACT_MONTHS 자동 계산. 수정 가능</li>
 *   <li>일시정지/해지 사유 필수 (운영 추적)</li>
 *   <li>수정 가능 필드: END_DATE / INSTALL_ADDRESS. 사실관계 (CUSTOMER/PRODUCT/START/MONTHLY_FEE) 불변</li>
 *   <li>신규 계약 차단: 상품 USE_YN='Y' AND 장비 USE_YN='Y' AND 장비 활성 계약 수 &lt; STOCK_QTY (Service 검증)</li>
 * </ul>
 */
@Entity
@Table(name = "CT_CONTRACT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Contract extends BaseAuditEntity {

    public static final String STATUS_ACTIVE     = "ACTIVE";
    public static final String STATUS_SUSPENDED  = "SUSPENDED";
    public static final String STATUS_TERMINATED = "TERMINATED";

    /** allow-list — 상태 전이 가능 조건. */
    public static final Set<String> SUSPENDABLE_STATUS  = Set.of(STATUS_ACTIVE);
    public static final Set<String> RESUMABLE_STATUS    = Set.of(STATUS_SUSPENDED);
    public static final Set<String> TERMINATABLE_STATUS = Set.of(STATUS_ACTIVE, STATUS_SUSPENDED);

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_ct_contract")
    @SequenceGenerator(name = "seq_ct_contract", sequenceName = "SEQ_CT_CONTRACT", allocationSize = 50)
    @Column(name = "CONTRACT_ID")
    private Long contractId;

    @Column(name = "CONTRACT_NO", length = 50, nullable = false, unique = true)
    private String contractNo;

    @Column(name = "CUSTOMER_ID", nullable = false, updatable = false)
    private Long customerId;

    @Column(name = "PRODUCT_ID", nullable = false, updatable = false)
    private Long productId;

    @Column(name = "MONTHLY_FEE", nullable = false, updatable = false)
    private Long monthlyFee;

    @Column(name = "START_DATE", nullable = false, updatable = false)
    private LocalDate startDate;

    @Column(name = "END_DATE", nullable = false)
    private LocalDate endDate;

    @Column(name = "INSTALL_ADDRESS", length = 500, nullable = false)
    private String installAddress;

    @Column(name = "CONTRACT_STATUS", length = 20, nullable = false)
    private String contractStatus;

    @Column(name = "SUSPENDED_AT")
    private LocalDateTime suspendedAt;

    @Column(name = "SUSPEND_REASON", length = 1000)
    private String suspendReason;

    @Column(name = "RESUMED_AT")
    private LocalDateTime resumedAt;

    @Column(name = "TERMINATED_AT")
    private LocalDateTime terminatedAt;

    @Column(name = "TERMINATE_REASON", length = 1000)
    private String terminateReason;

    @Builder
    private Contract(String contractNo, Long customerId, Long productId,
                     Long monthlyFee, LocalDate startDate, LocalDate endDate,
                     String installAddress) {
        this.contractNo     = contractNo;
        this.customerId     = customerId;
        this.productId      = productId;
        this.monthlyFee     = monthlyFee;
        this.startDate      = startDate;
        this.endDate        = endDate;
        this.installAddress = installAddress;
        this.contractStatus = STATUS_ACTIVE;
    }

    // ===== 도메인 행위 (Service 가 사전 validate 후 호출) =====

    /** 일시정지. Service 가 SUSPENDABLE_STATUS 검증 후 호출. */
    public void suspend(String reason) {
        this.contractStatus = STATUS_SUSPENDED;
        this.suspendedAt    = LocalDateTime.now();
        this.suspendReason  = reason;
    }

    /** 재개. Service 가 RESUMABLE_STATUS 검증 후 호출. */
    public void resume() {
        this.contractStatus = STATUS_ACTIVE;
        this.resumedAt      = LocalDateTime.now();
    }

    /** 해지. Service 가 TERMINATABLE_STATUS 검증 후 호출. */
    public void terminate(String reason) {
        this.contractStatus  = STATUS_TERMINATED;
        this.terminatedAt    = LocalDateTime.now();
        this.terminateReason = reason;
    }

    // ===== 일반 수정 (사실관계 외) =====
    public void changeEndDate(LocalDate endDate)            { this.endDate        = endDate; }
    public void changeInstallAddress(String installAddress) { this.installAddress = installAddress; }

    /** 등록 직후 자동 채번 적용 — Service.register 가 saved.contractId 받은 뒤 호출. */
    public void assignContractNo(String contractNo) { this.contractNo = contractNo; }
}
