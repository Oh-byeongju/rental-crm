package com.rental.domain.overdue.repository;

import com.rental.domain.overdue.entity.Overdue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OverdueRepository extends JpaRepository<Overdue, Long> {

    /** 한 청구당 한 연체 레코드 (재발생 시 reopen). UNIQUE (BILLING_ID). */
    Optional<Overdue> findByBillingId(Long billingId);

    /**
     * 검색 페이징.
     * 인덱스 활용:
     *  - IDX_BL_OVERDUE_CUSTOMER_RESOLVED (CUSTOMER_ID, RESOLVED_AT) — 고객별 미해결 조회
     *  - IDX_BL_OVERDUE_DAYS (OVERDUE_DAYS DESC) — 장기 연체자
     */
    @Query("""
        select o from Overdue o
         where (:customerId   is null or o.customerId = :customerId)
           and (:billingId    is null or o.billingId  = :billingId)
           and (:resolvedOnly is null
                or (:resolvedOnly = 'Y' and o.resolvedAt is not null)
                or (:resolvedOnly = 'N' and o.resolvedAt is null))
           and (:minDays      is null or o.overdueDays >= :minDays)
         order by o.overdueDays desc, o.overdueId desc
        """)
    Page<Overdue> search(@Param("customerId")   Long customerId,
                         @Param("billingId")    Long billingId,
                         @Param("resolvedOnly") String resolvedOnly,
                         @Param("minDays")      Integer minDays,
                         Pageable pageable);
}
