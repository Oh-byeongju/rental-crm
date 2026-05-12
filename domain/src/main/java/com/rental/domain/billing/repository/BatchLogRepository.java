package com.rental.domain.billing.repository;

import com.rental.domain.billing.entity.BatchLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BatchLogRepository extends JpaRepository<BatchLog, Long> {

    /**
     * 멱등성 검증 — 동일 (batchType, billingMonth) 의 이력 존재 여부.
     * UNIQUE (BATCH_TYPE, BILLING_MONTH) 제약. Ch.1 BILLING_CREATE 배치 중복 실행 차단.
     */
    Optional<BatchLog> findByBatchTypeAndBillingMonth(String batchType, String billingMonth);

    /** 검색 페이징. */
    @Query("""
        select b from BatchLog b
         where (:batchType   is null or b.batchType   = :batchType)
           and (:batchStatus is null or b.batchStatus = :batchStatus)
         order by b.startedAt desc, b.batchLogId desc
        """)
    Page<BatchLog> search(@Param("batchType")   String batchType,
                          @Param("batchStatus") String batchStatus,
                          Pageable pageable);
}
