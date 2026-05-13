package com.rental.batch.trigger.service;

import com.rental.batch.billing.service.BillingCreateService;
import com.rental.batch.trigger.dto.BatchRunRequest;
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
 * 배치 실행 dispatcher. {@code @Async} 로 별 스레드, REQUIRES_NEW 로 tx 분리.
 *
 * <p>시나리오별 분기:
 * <ul>
 *   <li>{@code DUMMY_*} — 내부 처리 (Step 5)</li>
 *   <li>{@code BILLING_CREATE} — {@link BillingCreateService#createMonthly} 위임 (Step 7)</li>
 * </ul>
 *
 * <p>실패 처리 한계 (Step 7-B 시점):
 * BILLING_CREATE 실패 시 같은 tx 안의 batchLog INSERT 도 rollback → 시도 흔적이 안 남음.
 * Step 7-C 에서 marking service 분리 (REQUIRES_NEW) 검토.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchRunnerService {

    private final BatchLogRepository batchLogRepository;
    private final BillingCreateService billingCreateService;

    @Async("batchTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void run(String batchType, BatchRunRequest req) {
        AuditContext.set(AuditContext.AuditInfo.defaults());
        try {
            switch (batchType) {
                case BatchLog.TYPE_DUMMY_SUCCESS, BatchLog.TYPE_DUMMY_FAIL ->
                        runDummy(batchType);
                case BatchLog.TYPE_BILLING_CREATE ->
                        billingCreateService.createMonthly(req.billingMonth(), req.roundNo(), req.strategy());
                default ->
                        log.warn("[batch] unhandled batchType={}", batchType);
            }
        } finally {
            AuditContext.clear();
        }
    }

    private void runDummy(String batchType) {
        var batchLog = batchLogRepository.save(
                BatchLog.builder().batchType(batchType).build()
        );
        batchLog.addProcess(true);

        if (BatchLog.TYPE_DUMMY_SUCCESS.equals(batchType)) {
            sleep(500);
            batchLog.markCompleted();
        } else {
            sleep(200);
            batchLog.markFailed("DUMMY_FAIL 시나리오 — 의도된 실패 (Step 5 검증)");
        }
        log.info("[batch] {} done — status={} duration={}ms",
                 batchType, batchLog.getBatchStatus(), batchLog.getDurationMs());
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
