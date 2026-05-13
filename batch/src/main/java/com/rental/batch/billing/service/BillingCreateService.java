package com.rental.batch.billing.service;

import com.rental.batch.billing.strategy.BillingInsertStrategy;
import com.rental.domain.billing.entity.BatchLog;
import com.rental.domain.contract.entity.Contract;
import com.rental.domain.contract.repository.ContractRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Ch.1 청구 일괄 생성 dispatcher (Step 7-C-2 리팩터).
 *
 * <p>**외부 tx 없음.** batchLog 작업은 {@link BatchLogManager} (REQUIRES_NEW), strategy 자체 tx.
 * <ul>
 *   <li>R1/R2/R3 strategy = 각자 클래스 level @Transactional — 한 번 호출 = 한 tx</li>
 *   <li>R4 ChunkCommitStrategy = tx 없음, chunk 마다 REQUIRES_NEW 자체 발동</li>
 * </ul>
 *
 * <p>실패 시 strategy tx 만 rollback. batchLog 행은 BatchLogManager 가 REQUIRES_NEW 로 commit 한 상태 보존.
 */
@Slf4j
@Service
public class BillingCreateService {

    private final ContractRepository contractRepository;
    private final BatchLogManager batchLogManager;
    private final Map<String, BillingInsertStrategy> strategiesByName;

    public BillingCreateService(ContractRepository contractRepository,
                                BatchLogManager batchLogManager,
                                List<BillingInsertStrategy> strategies) {
        this.contractRepository = contractRepository;
        this.batchLogManager = batchLogManager;
        this.strategiesByName = strategies.stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        BillingInsertStrategy::name, s -> s));
    }

    public void createMonthly(String billingMonth, Integer roundNo, String strategyName) {
        var strategy = strategiesByName.get(strategyName);
        if (strategy == null) {
            throw new IllegalArgumentException("unknown strategy: " + strategyName
                    + " (available: " + strategiesByName.keySet() + ")");
        }

        var batchLogId = batchLogManager.start(BatchLog.TYPE_BILLING_CREATE, billingMonth, roundNo, strategyName);

        try {
            log.info("[billing-create] R{} start — month={} strategy={} batchLogId={}",
                     roundNo, billingMonth, strategyName, batchLogId);

            // findAllActive() 는 SimpleJpaRepository 의 @Transactional(readOnly=true) 로 자체 tx.
            List<Contract> active = contractRepository.findAllActive();
            batchLogManager.markTarget(batchLogId, active.size());

            int success = strategy.execute(active, batchLogId, billingMonth);

            batchLogManager.complete(batchLogId, active.size(), success, active.size() - success);
            log.info("[billing-create] R{} done — target={} success={}", roundNo, active.size(), success);
        } catch (RuntimeException e) {
            log.error("[billing-create] R{} failed — month={} strategy={}",
                      roundNo, billingMonth, strategyName, e);
            batchLogManager.fail(batchLogId, e.getMessage());
            throw e;
        }
    }
}
