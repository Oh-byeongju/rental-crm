package com.rental.batch.billing.service;

import com.rental.batch.billing.strategy.BillingInsertStrategy;
import com.rental.domain.billing.entity.BatchLog;
import com.rental.domain.billing.repository.BatchLogRepository;
import com.rental.domain.contract.entity.Contract;
import com.rental.domain.contract.repository.ContractRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Ch.1 청구 일괄 생성 — 6 라운드 측정의 본 서비스.
 *
 * <p>흐름:
 * <ol>
 *   <li>{@code BL_BATCH_LOG} INSERT (RUNNING) — strategy / round 메타 박음</li>
 *   <li>활성 계약 전체 로드</li>
 *   <li>strategy 실행 (R1 single-save / R2 chunk-flush ...)</li>
 *   <li>카운터 일괄 update + COMPLETED</li>
 * </ol>
 *
 * <p>호출 측 {@link com.rental.batch.trigger.service.BatchRunnerService} 가 {@code @Async + REQUIRES_NEW}.
 * 본 메서드는 그 트랜잭션에 합류 (PROPAGATION.REQUIRED).
 */
@Slf4j
@Service
public class BillingCreateService {

    private final ContractRepository contractRepository;
    private final BatchLogRepository batchLogRepository;
    private final Map<String, BillingInsertStrategy> strategiesByName;

    /**
     * Spring 가 같은 인터페이스의 모든 구현 bean 을 List 로 주입 후, 본 생성자에서 name() → strategy 맵으로 변환.
     */
    public BillingCreateService(ContractRepository contractRepository,
                                BatchLogRepository batchLogRepository,
                                List<BillingInsertStrategy> strategies) {
        this.contractRepository = contractRepository;
        this.batchLogRepository = batchLogRepository;
        this.strategiesByName = strategies.stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        BillingInsertStrategy::name, s -> s));
    }

    @Transactional
    public void createMonthly(String billingMonth, Integer roundNo, String strategyName) {
        var strategy = strategiesByName.get(strategyName);
        if (strategy == null) {
            throw new IllegalArgumentException("unknown strategy: " + strategyName
                    + " (available: " + strategiesByName.keySet() + ")");
        }

        var batchLog = batchLogRepository.save(BatchLog.builder()
                .batchType(BatchLog.TYPE_BILLING_CREATE)
                .billingMonth(billingMonth)
                .roundNo(roundNo)
                .batchParams("{\"strategy\":\"" + strategyName + "\"}")
                .build());
        var batchLogId = batchLog.getBatchLogId();

        try {
            log.info("[billing-create] R{} start — month={} strategy={} batchLogId={}",
                     roundNo, billingMonth, strategyName, batchLogId);

            List<Contract> active = contractRepository.findAllActive();
            batchLog.markTargetCount(active.size());

            int success = strategy.execute(active, batchLogId, billingMonth);

            batchLog.setCounters(active.size(), success, active.size() - success);
            batchLog.markCompleted();

            log.info("[billing-create] R{} done — target={} success={} duration={}ms",
                     roundNo, active.size(), success, batchLog.getDurationMs());
        } catch (RuntimeException e) {
            // 트랜잭션 rollback 되어도 batchLog 의 markFailed 는 별도 트랜잭션 필요할 수 있음.
            // 현재 단순화 — 호출 측 BatchRunnerService 가 markFailed 처리 (REQUIRES_NEW 라 catch 가 별도 작동)
            log.error("[billing-create] R{} failed — month={} strategy={}",
                      roundNo, billingMonth, strategyName, e);
            throw e;
        }
    }
}
