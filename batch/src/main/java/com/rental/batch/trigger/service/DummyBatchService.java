package com.rental.batch.trigger.service;

import com.rental.domain.billing.entity.BatchLog;
import com.rental.domain.billing.repository.BatchLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADR-014 Step 5 — 통신 뼈대 검증용 더미 시나리오 실행.
 *
 * <p>{@link BatchRunnerService} 에서 분리한 이유: {@code save()} 후 엔티티 dirty-checking 으로
 * {@code markCompleted/markFailed} 를 flush 하려면 자체 tx 가 필요하다. 그런데 {@code run()} 은
 * **무 tx 여야** 한다 (run() 에 tx 를 두면 BILLING_CREATE strategy 가 합류 → 거짓 COMPLETED 회귀,
 * ADR-014 §3-1 / R5). private 메서드 {@code @Transactional} 은 self-invocation 으로 안 먹으므로
 * 별도 bean 으로 분리해 프록시 tx 를 보장한다.
 *
 * <p>Step 7 실 시나리오 안정화 후 제거 후보 ({@link BatchLog} Javadoc 참조).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DummyBatchService {

    private final BatchLogRepository batchLogRepository;

    @Transactional
    public void runDummy(String batchType) {
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
