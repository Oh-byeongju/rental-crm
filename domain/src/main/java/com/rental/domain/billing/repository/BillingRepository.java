package com.rental.domain.billing.repository;

import com.rental.domain.billing.entity.Billing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface BillingRepository extends JpaRepository<Billing, Long> {

    boolean existsByContractIdAndBillingMonth(Long contractId, String billingMonth);

    // ===== 대시보드 집계 (04 §9) =====

    /** 이번 달 청구 수 — BILLING_MONTH = 현재월. */
    long countByBillingMonth(String billingMonth);

    /** 미납 수 — BILLING_STATUS IN (UNPAID, OVERDUE). */
    long countByBillingStatusIn(java.util.Collection<String> statuses);

    /** 미납 금액 합계 — 동일 조건. coalesce 로 0 보장(null 방지). */
    @Query("select coalesce(sum(b.billingAmount), 0) from Billing b where b.billingStatus in :statuses")
    long sumAmountByBillingStatusIn(@Param("statuses") java.util.Collection<String> statuses);

    /**
     * 연체 대상 조회 — UNPAID + DUE_DATE 지남.
     * `IDX_BL_BILLING_STATUS_DUE (BILLING_STATUS, DUE_DATE)` 활용. Ch.1 OVERDUE_UPDATE 배치에서 사용.
     */
    @Query("""
        select b from Billing b
         where b.billingStatus = 'UNPAID'
           and b.dueDate < :today
        """)
    java.util.List<Billing> findOverdueCandidates(@Param("today") LocalDate today);

    /**
     * 검색 페이징 — 일반 조회 화면.
     * 인덱스 활용:
     *  - IDX_BL_BILLING_CUSTOMER (CUSTOMER_ID, BILLING_MONTH DESC) — 고객별 최근 청구
     *  - IDX_BL_BILLING_MONTH (BILLING_MONTH, BILLING_STATUS) — 월별 통계
     */
    @Query("""
        select b from Billing b
         where (:billingNo     is null or b.billingNo     like concat('%', :billingNo, '%'))
           and (:customerId    is null or b.customerId    = :customerId)
           and (:contractId    is null or b.contractId    = :contractId)
           and (:billingMonth  is null or b.billingMonth  = :billingMonth)
           and (:billingStatus is null or b.billingStatus = :billingStatus)
         order by b.billingId desc
        """)
    Page<Billing> search(@Param("billingNo")     String billingNo,
                         @Param("customerId")    Long customerId,
                         @Param("contractId")    Long contractId,
                         @Param("billingMonth")  String billingMonth,
                         @Param("billingStatus") String billingStatus,
                         Pageable pageable);
}
