package com.rental.batch.billing.service;

import com.rental.domain.billing.entity.BatchLog;
import com.rental.domain.billing.repository.BatchLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * BL_BATCH_LOG 의 INSERT/UPDATE 를 외부 트랜잭션과 분리한 marking service.
 *
 * <p>왜 분리?
 * <ul>
 *   <li>R4 chunk-commit strategy 가 chunk 마다 REQUIRES_NEW 로 BL_BILLING INSERT 시,
 *       BL_BATCH_LOG INSERT 가 같은 외부 tx 안에 묶여 있으면 FK BATCH_LOG_ID 참조 실패 (ORA-02291).
 *       → batchLog 작업도 별도 REQUIRES_NEW 로 즉시 commit 시켜야 함.</li>
 *   <li>실패 시 batchLog 행 보존 (외부 tx rollback 영향 회피) — 시도 흔적 유지.</li>
 * </ul>
 *
 * <p>각 메서드 = 1 tx (REQUIRES_NEW). 호출 측 tx 가 있으면 suspend.
 */
@Service
@RequiredArgsConstructor
public class BatchLogManager {

    private final BatchLogRepository batchLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long start(String batchType, String billingMonth, Integer roundNo, String strategyName) {
        var batchLog = batchLogRepository.save(BatchLog.builder()
                .batchType(batchType)
                .billingMonth(billingMonth)
                .roundNo(roundNo)
                .batchParams("{\"strategy\":\"" + strategyName + "\"}")
                .build());
        return batchLog.getBatchLogId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markTarget(Long batchLogId, int targetCount) {
        var batchLog = batchLogRepository.findById(batchLogId).orElseThrow();
        batchLog.markTargetCount(targetCount);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(Long batchLogId, int processed, int success, int fail) {
        var batchLog = batchLogRepository.findById(batchLogId).orElseThrow();
        batchLog.setCounters(processed, success, fail);
        batchLog.markCompleted();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(Long batchLogId, String errorMsg) {
        var batchLog = batchLogRepository.findById(batchLogId).orElseThrow();
        batchLog.markFailed(errorMsg);
    }
}
