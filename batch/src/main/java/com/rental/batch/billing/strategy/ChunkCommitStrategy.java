package com.rental.batch.billing.strategy;

import com.rental.batch.billing.BillingFactory;
import com.rental.domain.billing.repository.BillingRepository;
import com.rental.domain.contract.entity.Contract;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * R4 — chunk commit. 1000건 마다 자체 트랜잭션 commit.
 *
 * <p>특징:
 * <ul>
 *   <li>**클래스 level @Transactional 없음** — chunk 마다 자체 tx 시작</li>
 *   <li>{@code TransactionTemplate.execute(REQUIRES_NEW)} 로 chunk 마다 new tx</li>
 *   <li>UNDO 부담 ↓ — 매 1000건 commit 시점에 UNDO 해제</li>
 *   <li>실패 시 직전 commit 까지만 보존 — 재시작 친화적 (이번 사이클은 재시작 로직 X)</li>
 * </ul>
 *
 * <p>R5 (UNDO 폭주) 의 사전 작업 — R1 한 tx 5만건 vs R4 50 tx × 1000건 차이 비교.
 */
@Component
@RequiredArgsConstructor
public class ChunkCommitStrategy implements BillingInsertStrategy {

    private static final int CHUNK = 1000;

    private final BillingRepository billingRepository;
    private final PlatformTransactionManager transactionManager;

    @Override
    public String name() { return "chunk-commit"; }

    @Override
    public int execute(List<Contract> contracts, Long batchLogId, String billingMonth) {
        var template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        int total = 0;
        for (int start = 0; start < contracts.size(); start += CHUNK) {
            int end = Math.min(start + CHUNK, contracts.size());
            List<Contract> sub = contracts.subList(start, end);
            Integer saved = template.execute(status -> {
                for (Contract c : sub) {
                    billingRepository.save(BillingFactory.create(c, batchLogId, billingMonth));
                }
                return sub.size();
            });
            total += saved != null ? saved : 0;
        }
        return total;
    }
}
