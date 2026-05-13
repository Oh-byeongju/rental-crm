package com.rental.batch.billing.strategy;

import com.rental.batch.billing.BillingFactory;
import com.rental.domain.billing.repository.BillingRepository;
import com.rental.domain.contract.entity.Contract;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * R2 — 건별 {@code save()} + 1000건마다 {@code flush + clear}.
 *
 * <p>예상 동작:
 * <ul>
 *   <li>1차 캐시가 1000건 단위로 비워짐 → 메모리 안정</li>
 *   <li>{@code jdbc.batch_size=1000} 와 chunk 크기 일치 → batch INSERT 1회/chunk</li>
 *   <li>R1 대비 메모리 ↓, INSERT throughput ↑ (예상)</li>
 * </ul>
 *
 * <p>주의: clear() 가 contract 객체도 detach 시키지만 List iterator 는 reference 유지 → getter OK.
 */
@Component
@RequiredArgsConstructor
public class ChunkFlushClearStrategy implements BillingInsertStrategy {

    private static final int CHUNK = 1000;

    private final BillingRepository billingRepository;
    private final EntityManager entityManager;

    @Override
    public String name() { return "chunk-flush"; }

    @Override
    public int execute(List<Contract> contracts, Long batchLogId, String billingMonth) {
        int n = 0;
        for (Contract c : contracts) {
            billingRepository.save(BillingFactory.create(c, batchLogId, billingMonth));
            n++;
            if (n % CHUNK == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        entityManager.flush();
        entityManager.clear();
        return n;
    }
}
