package com.rental.batch.trigger.service;

import com.rental.domain.billing.entity.BatchLog;
import com.rental.domain.billing.repository.BatchLogRepository;
import com.rental.domain.common.audit.AuditContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 배치 실행기 (ADR-014 Step 5 — 통신 뼈대 검증용 더미).
 *
 * <p>Step 7 Ch.1 청구 배치 본 구현 시점에 시나리오별 Runner 로 분리될 가능성 있음.
 * 현재는 더미 2종 (DUMMY_SUCCESS / DUMMY_FAIL) 만 처리.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchRunnerService {

    private final BatchLogRepository batchLogRepository;

    /**
     * 비동기 실행. caller 는 즉시 반환받고, 본 메서드가 별도 스레드에서 진행.
     * <p>{@code @Async} 가 동작하려면 다른 빈에서 호출되어야 함 (self-invocation X).
     * <p>{@code REQUIRES_NEW} — 호출 측 트랜잭션과 분리 (caller 는 트랜잭션 없음 / 별 스레드).
     */
    @Async("batchTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void run(String batchType) {
        // ThreadLocal AuditContext 는 @Async 스레드로 전파 안 됨 → SYSTEM 기본값 명시
        AuditContext.set(AuditContext.AuditInfo.defaults());
        try {
            var batchLog = batchLogRepository.save(
                    BatchLog.builder().batchType(batchType).build()
            );
            batchLog.addProcess(true);   // dummy — 1건 처리한 것으로 가장

            switch (batchType) {
                case BatchLog.TYPE_DUMMY_SUCCESS -> {
                    sleep(500);
                    batchLog.markCompleted();
                }
                case BatchLog.TYPE_DUMMY_FAIL -> {
                    sleep(200);
                    batchLog.markFailed("DUMMY_FAIL 시나리오 — 의도된 실패 (Step 5 검증)");
                }
                default -> batchLog.markFailed("unknown batchType: " + batchType);
            }
            log.info("[batch] {} done — status={} duration={}ms",
                     batchType, batchLog.getBatchStatus(), batchLog.getDurationMs());
        } finally {
            AuditContext.clear();
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
