package com.rental.domain.visit.repository;

import com.rental.domain.visit.entity.Visit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    /**
     * 기사별 같은 날 일정 카운트 — 5건 초과 차단 검증용.
     * `IDX_CT_VISIT_ENGINEER_DATE` 활용. CANCELLED 제외.
     */
    @Query("""
        select count(v) from Visit v
         where v.engineerId    = :engineerId
           and v.scheduledDate = :scheduledDate
           and v.visitStatus  <> 'CANCELLED'
        """)
    long countByEngineerIdAndScheduledDate(@Param("engineerId") Long engineerId,
                                           @Param("scheduledDate") LocalDate scheduledDate);

    /** 검색 페이징. */
    @Query("""
        select v from Visit v
         where (:contractId   is null or v.contractId   = :contractId)
           and (:engineerId   is null or v.engineerId   = :engineerId)
           and (:visitType    is null or v.visitType    = :visitType)
           and (:visitStatus  is null or v.visitStatus  = :visitStatus)
           and (:dateFrom     is null or v.scheduledDate >= :dateFrom)
           and (:dateTo       is null or v.scheduledDate <= :dateTo)
         order by v.scheduledDate desc, v.visitId desc
        """)
    Page<Visit> search(@Param("contractId")  Long contractId,
                       @Param("engineerId")  Long engineerId,
                       @Param("visitType")   String visitType,
                       @Param("visitStatus") String visitStatus,
                       @Param("dateFrom")    LocalDate dateFrom,
                       @Param("dateTo")      LocalDate dateTo,
                       Pageable pageable);
}
