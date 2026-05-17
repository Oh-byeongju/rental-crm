package com.rental.domain.payment.repository;

import com.rental.domain.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** Toss Payments 결제 중복 방지 — `UK_BL_PAYMENT_TOSS_ORDER_ID` (NULL 허용). */
    boolean existsByTossOrderId(String tossOrderId);

    /**
     * 청구별 활성 수납 수 — Service.register 의 중복 수납 차단 검증용.
     * COMPLETED 상태만 카운트 (CANCELLED/REFUNDED 제외).
     */
    @Query("""
        select count(p) from Payment p
         where p.billingId = :billingId
           and p.paymentStatus = 'COMPLETED'
        """)
    long countActiveByBillingId(@Param("billingId") Long billingId);

    /**
     * 대시보드 집계 (04 §9) — 이번 달 수납액.
     * 수납일이 [start, end] 범위 + COMPLETED. coalesce 로 0 보장.
     */
    @Query("""
        select coalesce(sum(p.paymentAmount), 0) from Payment p
         where p.paymentStatus = 'COMPLETED'
           and p.paymentDate between :start and :end
        """)
    long sumCompletedAmountBetween(@Param("start") LocalDate start,
                                   @Param("end")   LocalDate end);

    /**
     * 검색 페이징.
     * 인덱스 활용:
     *  - IDX_BL_PAYMENT_BILLING (BILLING_ID, PAYMENT_STATUS) — 청구별 수납 조회
     *  - IDX_BL_PAYMENT_CUSTOMER_DATE (CUSTOMER_ID, PAYMENT_DATE DESC) — 고객 납부 이력
     *  - IDX_BL_PAYMENT_DATE_METHOD (PAYMENT_DATE, PAYMENT_METHOD) — 일별/수단별 통계
     */
    @Query("""
        select p from Payment p
         where (:paymentNo     is null or p.paymentNo     like concat('%', :paymentNo, '%'))
           and (:billingId     is null or p.billingId     = :billingId)
           and (:customerId    is null or p.customerId    = :customerId)
           and (:paymentMethod is null or p.paymentMethod = :paymentMethod)
           and (:paymentStatus is null or p.paymentStatus = :paymentStatus)
           and (:dateFrom      is null or p.paymentDate  >= :dateFrom)
           and (:dateTo        is null or p.paymentDate  <= :dateTo)
         order by p.paymentDate desc, p.paymentId desc
        """)
    Page<Payment> search(@Param("paymentNo")     String paymentNo,
                         @Param("billingId")     Long billingId,
                         @Param("customerId")    Long customerId,
                         @Param("paymentMethod") String paymentMethod,
                         @Param("paymentStatus") String paymentStatus,
                         @Param("dateFrom")      LocalDate dateFrom,
                         @Param("dateTo")        LocalDate dateTo,
                         Pageable pageable);
}
