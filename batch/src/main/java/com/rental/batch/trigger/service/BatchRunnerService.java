package com.rental.batch.trigger.service;

import com.rental.batch.billing.service.BillingCreateService;
import com.rental.batch.trigger.dto.BatchRunRequest;
import com.rental.domain.billing.entity.BatchLog;
import com.rental.domain.common.audit.AuditContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 배치 실행 dispatcher. {@code @Async} 로 별 스레드 (fire-and-forget, ADR-014 §통신).
 *
 * <p>⚠️ **run() 에 절대 {@code @Transactional} 을 붙이지 말 것** (ADR-014 §3-1 / R5 거짓 COMPLETED 회귀 가드).
 * 외부 tx 가 생기면 strategy 의 {@code @Transactional}(REQUIRED) 이 거기 합류해, 5만 INSERT 의
 * flush/commit 이 run() 의 commit 경계로 밀린다. 그 전에 {@code BatchLogManager.complete()}
 * (REQUIRES_NEW) 가 COMPLETED 를 먼저 commit → commit 단계에서 ORA-30036 등으로 데이터는 rollback
 * 인데 BL_BATCH_LOG 는 거짓 COMPLETED 로 남는다. BILLING_CREATE 는 **무 tx 로 진입**해 strategy 가
 * 자체 tx 를 commit/실패하게 하고, 실패는 {@code createMonthly()} 의 try/catch 로 잡혀 FAILED 기록.
 *
 * <p>DUMMY 시나리오는 save() 후 dirty-checking 에 자체 tx 가 필요 →
 * {@link DummyBatchService} (별도 bean {@code @Transactional}) 로 위임.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchRunnerService {

    private final BillingCreateService billingCreateService;
    private final DummyBatchService dummyBatchService;

    @Async("batchTaskExecutor")
    public void run(String batchType, BatchRunRequest req) {
        AuditContext.set(AuditContext.AuditInfo.defaults());
        try {
            switch (batchType) {
                case BatchLog.TYPE_DUMMY_SUCCESS, BatchLog.TYPE_DUMMY_FAIL ->
                        dummyBatchService.runDummy(batchType);
                case BatchLog.TYPE_BILLING_CREATE ->
                        billingCreateService.createMonthly(req.billingMonth(), req.roundNo(), req.strategy());
                default ->
                        log.warn("[batch] unhandled batchType={}", batchType);
            }
        } finally {
            AuditContext.clear();
        }
    }
}
