package com.rental.batch.billing.strategy;

import com.rental.domain.contract.entity.Contract;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * R6-C — 멱등성: Oracle {@code MERGE INTO}.
 *
 * <p>{@code WHEN NOT MATCHED THEN INSERT} 만 사용 → 존재하면 DB 가 알아서 스킵, 없으면 INSERT.
 * 멱등성을 <b>DB 엔진</b>에 위임. 애플리케이션은 예외 처리도(R6-A) 사전 조회도(R6-B) 안 함.
 *
 * <p>{@link BulkJdbcStrategy}(R3) 와 동일한 {@link JdbcTemplate#batchUpdate} 메커니즘 —
 * INSERT → MERGE 만 차이. 따라서 <b>R3 대비 delta = 순수 멱등성(ON 절 매칭) 오버헤드</b>.
 *
 * <p>참고: Oracle JDBC batch 모드는 행별 영향 건수 대신 {@code SUCCESS_NO_INFO(-2)} 를
 * 반환할 수 있어 result[] 로 신규 INSERT 수를 신뢰성 있게 셀 수 없다. 멱등성 검증은
 * 측정 절차에서 {@code SELECT COUNT(*) ... WHERE BILLING_MONTH=?} 가 정확히 50000 유지인지로 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MergeStrategy implements BillingInsertStrategy {

    private static final String SQL = """
        MERGE INTO BL_BILLING d
        USING (SELECT ? AS BILLING_NO, ? AS CONTRACT_ID, ? AS CUSTOMER_ID, ? AS BATCH_LOG_ID,
                      ? AS BILLING_MONTH, ? AS BILLING_AMOUNT, ? AS ISSUE_DATE, ? AS DUE_DATE,
                      ? AS REG_DTS
                 FROM DUAL) s
        ON (d.CONTRACT_ID = s.CONTRACT_ID AND d.BILLING_MONTH = s.BILLING_MONTH)
        WHEN NOT MATCHED THEN INSERT
            (BILLING_ID, BILLING_NO, CONTRACT_ID, CUSTOMER_ID, BATCH_LOG_ID,
             BILLING_MONTH, BILLING_AMOUNT, ISSUE_DATE, DUE_DATE, BILLING_STATUS,
             FIRS_REG_PGM_ID, FIRS_REG_DTS, FIRS_REG_USER_ID, FIRS_REG_IP,
             FINA_REG_PGM_ID, FINA_REG_DTS, FINA_REG_USER_ID, FINA_REG_IP)
            VALUES
            (SEQ_BL_BILLING.NEXTVAL, s.BILLING_NO, s.CONTRACT_ID, s.CUSTOMER_ID, s.BATCH_LOG_ID,
             s.BILLING_MONTH, s.BILLING_AMOUNT, s.ISSUE_DATE, s.DUE_DATE, 'UNPAID',
             'SYSTEM', s.REG_DTS, 'SYSTEM', '127.0.0.1',
             'SYSTEM', s.REG_DTS, 'SYSTEM', '127.0.0.1')
        """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String name() { return "merge"; }

    @Override
    @Transactional
    public int execute(List<Contract> contracts, Long batchLogId, String billingMonth) {
        var yyyymm    = billingMonth.replace("-", "");
        var issueDate = Timestamp.valueOf(LocalDate.parse(billingMonth + "-01").atStartOfDay());
        var dueDate   = Timestamp.valueOf(LocalDate.parse(billingMonth + "-25").atStartOfDay());
        var now       = Timestamp.valueOf(LocalDateTime.now());

        jdbcTemplate.batchUpdate(SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Contract c = contracts.get(i);
                ps.setString    (1,  "BL-" + yyyymm + "-" + String.format("%010d", c.getContractId()));
                ps.setLong      (2,  c.getContractId());
                ps.setLong      (3,  c.getCustomerId());
                ps.setLong      (4,  batchLogId);
                ps.setString    (5,  billingMonth);
                ps.setLong      (6,  c.getMonthlyFee());
                ps.setTimestamp (7,  issueDate);
                ps.setTimestamp (8,  dueDate);
                ps.setTimestamp (9,  now);
            }

            @Override
            public int getBatchSize() { return contracts.size(); }
        });
        log.info("[R6-C merge] processed={} (WHEN NOT MATCHED — 신규만 INSERT, 중복 자동 스킵)",
                 contracts.size());
        return contracts.size();   // 멱등 성공 = 원하는 상태 도달 건수 (MERGE 는 무오류 보장)
    }
}
