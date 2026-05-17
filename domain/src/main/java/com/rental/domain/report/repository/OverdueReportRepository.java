package com.rental.domain.report.repository;

import com.rental.domain.billing.entity.Billing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.QueryHint;
import java.util.stream.Stream;

/**
 * 미납 현황 리포트 전용 조회 — Ch.2 쿼리 튜닝 학습.
 *
 * <p><b>채택 쿼리 = JOIN 형</b> (스칼라 서브쿼리 → JOIN 튜닝 후). naive 스칼라 서브쿼리
 * 형은 운영 코드에 싣지 않고 측정 리포트
 * (`docs/perf-reports/2026-05-17-overdue-report-query-tuning.md`) 에 EXPLAIN PLAN 과 함께 보존.
 *
 * <p>"미납 현황" = 미수납 청구 = {@code BILLING_STATUS IN ('UNPAID','OVERDUE')}
 * (04 기능명세 §대시보드 정의). 청구 + 고객 + 계약 INNER JOIN + 연체 LEFT JOIN.
 *
 * <p>{@link Billing} 을 도메인 타입으로 바인딩(구동 테이블)하되 JpaRepository 가 아닌
 * 최소 {@link Repository} 만 확장 — 이 리포지토리는 조회 전용. (같은 엔티티에 대한
 * 복수 리포지토리는 Spring Data 허용.)
 */
public interface OverdueReportRepository extends Repository<Billing, Long> {

    String SELECT_COLS = """
        SELECT b.BILLING_NO      AS billingNo,
               b.BILLING_MONTH   AS billingMonth,
               c.CUSTOMER_NO     AS customerNo,
               c.CUSTOMER_NAME   AS customerName,
               c.PHONE           AS phone,
               ct.CONTRACT_NO    AS contractNo,
               b.BILLING_AMOUNT  AS billingAmount,
               b.DUE_DATE        AS dueDate,
               b.BILLING_STATUS  AS billingStatus,
               o.OVERDUE_DAYS    AS overdueDays
        """;

    String FROM_WHERE = """
          FROM BL_BILLING b
          JOIN CT_CUSTOMER  c  ON c.CUSTOMER_ID  = b.CUSTOMER_ID
          JOIN CT_CONTRACT  ct ON ct.CONTRACT_ID = b.CONTRACT_ID
          LEFT JOIN BL_OVERDUE o ON o.BILLING_ID = b.BILLING_ID
         WHERE b.BILLING_STATUS IN ('UNPAID', 'OVERDUE')
           AND (:billingMonth  IS NULL OR b.BILLING_MONTH  = :billingMonth)
           AND (:status        IS NULL OR b.BILLING_STATUS = :status)
           AND (:customerName  IS NULL OR c.CUSTOMER_NAME LIKE '%' || :customerName || '%')
        """;

    /** 화면 페이징 조회. */
    @Query(value  = SELECT_COLS + FROM_WHERE + " ORDER BY b.BILLING_ID DESC",
           countQuery = "SELECT COUNT(*) " + FROM_WHERE,
           nativeQuery = true)
    Page<OverdueReportView> searchPaged(@Param("billingMonth") String billingMonth,
                                        @Param("status")       String status,
                                        @Param("customerName") String customerName,
                                        Pageable pageable);

    /**
     * 엑셀 스트리밍용 커서 조회 — 결과를 메모리에 다 적재하지 않고 행 단위 소비.
     * {@code fetchSize} 힌트로 JDBC fetch 단위 지정. 호출 측은 readOnly tx 안에서
     * try-with-resources 로 Stream 소비 후 close 필수.
     */
    @QueryHints(@QueryHint(name = "org.hibernate.fetchSize", value = "500"))
    @Query(value = SELECT_COLS + FROM_WHERE + " ORDER BY b.BILLING_ID DESC",
           nativeQuery = true)
    Stream<OverdueReportView> streamAll(@Param("billingMonth") String billingMonth,
                                        @Param("status")       String status,
                                        @Param("customerName") String customerName);

    /** 0건 가드 — 엑셀 빈 파일 대신 오류 응답용 사전 카운트. */
    @Query(value = "SELECT COUNT(*) " + FROM_WHERE, nativeQuery = true)
    long countMatching(@Param("billingMonth") String billingMonth,
                       @Param("status")       String status,
                       @Param("customerName") String customerName);
}
