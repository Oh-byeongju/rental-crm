package com.rental.batch.billing.strategy;

import com.rental.domain.contract.entity.Contract;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * R6-A — 멱등성: catch + continue.
 *
 * <p>각 계약을 단건 INSERT 시도 → {@code UK_BL_BILLING_CONTRACT_MONTH} / {@code UK_BL_BILLING_NO}
 * 위반(ORA-00001)을 {@link DuplicateKeyException} 으로 잡고 다음 row 로 진행.
 *
 * <p><b>왜 JDBC 인가 — JPA naive catch+continue 가 안 되는 이유:</b>
 * <ul>
 *   <li>JPA/Hibernate 는 제약 위반이 한 번 터지면 영속성 컨텍스트의 트랜잭션을
 *       <b>rollback-only</b> 로 마킹한다. 같은 tx 안에서 그 뒤 save() 를 아무리 try/catch 로
 *       감싸도 flush/commit 단계에서 전체가 터진다 → "건너뛰고 계속" 이 성립 안 함.</li>
 *   <li>raw JDBC 는 다르다. Oracle 은 <b>statement-level rollback</b> — 실패한 INSERT 한 문장만
 *       무효화되고 같은 connection/tx 의 직전 성공분과 이후 문장은 살아 있다. 그래서
 *       단일 tx 안에서 per-row try/catch 가 진짜로 동작한다.</li>
 *   <li>caught exception 은 {@code @Transactional} proxy 경계를 넘지 않으므로(여기서 잡고
 *       rethrow 안 함) Spring 이 tx 를 rollback-only 로 마킹하지 않는다. Hibernate 세션도
 *       건드리지 않는다(순수 JDBC) → 단일 tx 유지.</li>
 * </ul>
 *
 * <p>측정 포인트: 5만 건 중 전부 중복인 재실행에서 <b>예외 생성·SQL 예외 변환 비용 × 5만</b>.
 * SEQ_BL_BILLING.NEXTVAL 은 실패한 INSERT 에서도 소비된다(시퀀스는 비트랜잭셔널) — R3 와 동일하게 5만 회.
 *
 * <p>SQL 은 {@link BulkJdbcStrategy} 와 동일(단건 vs batch 차이만).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatchContinueStrategy implements BillingInsertStrategy {

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
    public String name() { return "catch-continue"; }

    @Override
    @Transactional
    public int execute(List<Contract> contracts, Long batchLogId, String billingMonth) {
        var yyyymm    = billingMonth.replace("-", "");
        var issueDate = Timestamp.valueOf(LocalDate.parse(billingMonth + "-01").atStartOfDay());
        var dueDate   = Timestamp.valueOf(LocalDate.parse(billingMonth + "-25").atStartOfDay());
        var now       = Timestamp.valueOf(LocalDateTime.now());

        int inserted = 0;
        int skipped  = 0;
        for (Contract c : contracts) {
            try {
                jdbcTemplate.update(SQL,
                        "BL-" + yyyymm + "-" + String.format("%010d", c.getContractId()),
                        c.getContractId(),
                        c.getCustomerId(),
                        batchLogId,
                        billingMonth,
                        c.getMonthlyFee(),
                        issueDate,
                        dueDate,
                        now,
                        now);
                inserted++;
            } catch (DuplicateKeyException dup) {
                skipped++;   // 이미 존재 — 멱등 no-op
            }
        }
        log.info("[R6-A catch-continue] inserted={} skipped={} total={}",
                 inserted, skipped, contracts.size());
        return inserted + skipped;   // 멱등 성공 = 원하는 상태 도달 건수
    }
}
