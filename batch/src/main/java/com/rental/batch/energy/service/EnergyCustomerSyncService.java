package com.rental.batch.energy.service;

import com.rental.batch.billing.service.BatchLogManager;
import com.rental.domain.billing.entity.BatchLog;
import com.rental.domain.common.audit.AuditContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Step 10 — `ENERGY_CUSTOMER_SYNC` : 외부(가상) 에너지사 고객 CSV 동기.
 *
 * <p>학습 포인트 (docs/100 §5-2):
 * <ul>
 *   <li>외부 시스템 → 파일(CSV) → bulk INSERT</li>
 *   <li>멱등성 — 외부 ID 기준 Oracle MERGE (upsert). CUSTOMER_NO = {@code ENG-<extId>} 가 키.
 *       재실행 시 MATCHED → UPDATE (신규 INSERT 없음) → CT_CUSTOMER ENG-* 건수 불변.</li>
 *   <li>실패 데이터 분리 (DLQ식) — 행별 검증 실패는 MERGE 에서 빼고 FAIL_COUNT +
 *       ERROR_MSG 다이제스트로 분리 기록 (별도 테이블 없음, BL_BATCH_LOG 활용 — §8 정합).</li>
 * </ul>
 *
 * <p>R6-C {@code MergeStrategy} 의 {@code JdbcTemplate#batchUpdate} + Oracle MERGE 패턴 차용.
 * 차이: insert-only(R6-C) → <b>MATCHED UPDATE + NOT MATCHED INSERT (full upsert)</b> (신규/변경 동기).
 * 감사 컬럼은 JPA 우회라 SQL 에 'ENERGY_SYNC'/'SYSTEM' 고정 (MergeStrategy 동일 방식).
 *
 * <p>CSV 가정: 단순 포맷(필드 내 콤마/따옴표 없음 — 통제된 내부 샘플). 운영 외부연계 시
 * CSV 라이브러리 도입은 별도(현 학습 범위 아님). 호출 측이 AuditContext 설정 책임
 * (수동=BatchRunnerService, 스케줄=본 클래스 scheduledSync).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnergyCustomerSyncService {

    /** 동기 고객은 로그인 안 함 — NOT NULL PASSWORD 자리표시(불가 해시). */
    private static final String PWD_PLACEHOLDER =
            "$2a$10$energySyncDisabledLoginPlaceholderHashAAAAAAAAAAAAAAAAAAAA";

    private static final String MERGE_SQL = """
        MERGE INTO CT_CUSTOMER d
        USING (SELECT ? AS CUSTOMER_NO, ? AS CUSTOMER_NAME, ? AS EMAIL,
                      ? AS PHONE, ? AS ADDRESS, ? AS REG_DTS FROM DUAL) s
        ON (d.CUSTOMER_NO = s.CUSTOMER_NO)
        WHEN MATCHED THEN UPDATE SET
            d.CUSTOMER_NAME    = s.CUSTOMER_NAME,
            d.PHONE            = s.PHONE,
            d.ADDRESS          = s.ADDRESS,
            d.FINA_REG_PGM_ID  = 'ENERGY_SYNC',
            d.FINA_REG_DTS     = s.REG_DTS,
            d.FINA_REG_USER_ID = 'SYSTEM',
            d.FINA_REG_IP      = '127.0.0.1'
        WHEN NOT MATCHED THEN INSERT
            (CUSTOMER_ID, CUSTOMER_NO, CUSTOMER_NAME, EMAIL, PASSWORD, PHONE, ADDRESS,
             TERMS_AGREE_YN, USE_YN, LOGIN_FAIL_CNT,
             FIRS_REG_PGM_ID, FIRS_REG_DTS, FIRS_REG_USER_ID, FIRS_REG_IP,
             FINA_REG_PGM_ID, FINA_REG_DTS, FINA_REG_USER_ID, FINA_REG_IP)
            VALUES
            (SEQ_CT_CUSTOMER.NEXTVAL, s.CUSTOMER_NO, s.CUSTOMER_NAME, s.EMAIL, '"""
        + PWD_PLACEHOLDER + """
        ', s.PHONE, s.ADDRESS,
             'Y', 'Y', 0,
             'ENERGY_SYNC', s.REG_DTS, 'SYSTEM', '127.0.0.1',
             'ENERGY_SYNC', s.REG_DTS, 'SYSTEM', '127.0.0.1')
        """;

    private final JdbcTemplate jdbcTemplate;
    private final BatchLogManager batchLogManager;
    private final ResourceLoader resourceLoader;

    @Value("${app.energy.csv-path}")
    private String csvPath;

    private record EnergyRow(String extId, String name, String email, String phone, String address) {}

    /** 매일 새벽 04:00 — docs/100 §5. 세션 중 미발화(정상). 수동은 BatchRunnerService 경유. */
    @Scheduled(cron = "0 0 4 * * *")
    public void scheduledSync() {
        AuditContext.set(AuditContext.AuditInfo.defaults());
        try {
            sync();
        } finally {
            AuditContext.clear();
        }
    }

    /** 외부 CSV → CT_CUSTOMER MERGE. 호출 측이 AuditContext 설정 보장. */
    public void sync() {
        var batchLogId = batchLogManager.start(
                BatchLog.TYPE_ENERGY_CUSTOMER_SYNC, null, null, "csv-merge");
        try {
            List<EnergyRow> valid = new ArrayList<>();
            List<String> fails = new ArrayList<>();
            int total = parse(valid, fails);
            batchLogManager.markTarget(batchLogId, total);
            log.info("[energy-sync] start csv={} total={} valid={} invalid={}",
                     csvPath, total, valid.size(), fails.size());

            if (!valid.isEmpty()) {
                merge(valid);
            }

            int success = valid.size();
            int fail = fails.size();
            if (fail == 0) {
                batchLogManager.complete(batchLogId, total, success, 0);
            } else {
                String digest = String.join(" | ", fails);
                if (digest.length() > 2000) digest = digest.substring(0, 2000);
                batchLogManager.completeWithErrors(batchLogId, total, success, fail, digest);
            }
            log.info("[energy-sync] done total={} success={} fail={}", total, success, fail);
        } catch (RuntimeException e) {
            log.error("[energy-sync] failed csv={}", csvPath, e);
            batchLogManager.fail(batchLogId, e.getMessage());
            throw e;
        }
    }

    /** CSV 읽어 valid/invalid 분리. 반환 = 총 데이터 행수. */
    private int parse(List<EnergyRow> valid, List<String> fails) {
        Resource resource = resourceLoader.getResource(csvPath);
        if (!resource.exists()) {
            throw new IllegalStateException("CSV 없음: " + csvPath);
        }
        int total = 0;
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean header = true;
            int lineNo = 0;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (header) { header = false; continue; }
                if (line.isBlank()) continue;
                total++;
                String[] f = line.split(",", -1);
                if (f.length < 5) { fails.add("L" + lineNo + " 컬럼부족"); continue; }
                String extId = f[0].trim(), name = f[1].trim(), email = f[2].trim(),
                       phone = f[3].trim(), address = f[4].trim();
                if (extId.isEmpty() || name.isEmpty() || email.isEmpty()
                        || phone.isEmpty() || address.isEmpty()) {
                    fails.add("L" + lineNo + " 필수누락(ext=" + extId + ")");
                    continue;
                }
                valid.add(new EnergyRow(extId, name, email, phone, address));
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException("CSV 읽기 실패: " + csvPath, e);
        }
        return total;
    }

    private void merge(List<EnergyRow> rows) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        jdbcTemplate.batchUpdate(MERGE_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                EnergyRow r = rows.get(i);
                ps.setString(1, "ENG-" + r.extId());
                ps.setString(2, r.name());
                ps.setString(3, r.email());
                ps.setString(4, r.phone());
                ps.setString(5, r.address());
                ps.setTimestamp(6, now);
            }

            @Override
            public int getBatchSize() { return rows.size(); }
        });
        log.info("[energy-sync] MERGE applied rows={} (MATCHED→UPDATE / NOT MATCHED→INSERT)",
                 rows.size());
    }
}
