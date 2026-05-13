package com.rental.batch.billing.strategy;

import com.rental.domain.contract.entity.Contract;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * R3 — JDBC bulk INSERT (JPA 우회).
 *
 * <p>JPA save() 가 아니라 {@link JdbcTemplate#batchUpdate} 로 PreparedStatement.addBatch + executeBatch 직접.
 * <ul>
 *   <li>영속성 컨텍스트 안 거침 → 1차 캐시 부담 0</li>
 *   <li>Hibernate dirty checking / @PreUpdate listener / Auditing 모두 우회 → 감사 컬럼 수동 set</li>
 *   <li>SEQ_BL_BILLING.NEXTVAL 도 SQL 안에 직접 박음 (allocationSize prefetch 우회)</li>
 * </ul>
 *
 * <p>예상: R1 (6.5초) 보다 빠를 것. 진짜 bulk 의 가치 비교.
 */
@Component
@RequiredArgsConstructor
public class BulkJdbcStrategy implements BillingInsertStrategy {

    private static final String SQL = """
        INSERT INTO BL_BILLING
            (BILLING_ID, BILLING_NO, CONTRACT_ID, CUSTOMER_ID, BATCH_LOG_ID,
             BILLING_MONTH, BILLING_AMOUNT, ISSUE_DATE, DUE_DATE, BILLING_STATUS,
             FIRS_REG_PGM_ID, FIRS_REG_DTS, FIRS_REG_USER_ID, FIRS_REG_IP,
             FINA_REG_PGM_ID, FINA_REG_DTS, FINA_REG_USER_ID, FINA_REG_IP)
        VALUES
            (SEQ_BL_BILLING.NEXTVAL, ?, ?, ?, ?,
             ?, ?, ?, ?, 'UNPAID',
             'SYSTEM', ?, 'SYSTEM', '127.0.0.1',
             'SYSTEM', ?, 'SYSTEM', '127.0.0.1')
        """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String name() { return "bulk-jdbc"; }

    @Override
    public int execute(List<Contract> contracts, Long batchLogId, String billingMonth) {
        var yyyymm = billingMonth.replace("-", "");
        var issueDate = Timestamp.valueOf(LocalDate.parse(billingMonth + "-01").atStartOfDay());
        var dueDate   = Timestamp.valueOf(LocalDate.parse(billingMonth + "-25").atStartOfDay());
        var now       = Timestamp.valueOf(LocalDateTime.now());

        int[] result = jdbcTemplate.batchUpdate(SQL, new BatchPreparedStatementSetter() {
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
                ps.setTimestamp (10, now);
            }

            @Override
            public int getBatchSize() { return contracts.size(); }
        });
        return result.length;
    }
}
