package com.rental.batch.billing.strategy;

import com.rental.batch.billing.BillingFactory;
import com.rental.domain.billing.repository.BillingRepository;
import com.rental.domain.contract.entity.Contract;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * R1 — 건별 {@code save()}. flush/clear 안 함.
 *
 * <p>예상 동작:
 * <ul>
 *   <li>Hibernate 1차 캐시에 5만 청구 객체 + 5만 contract 객체 누적 → 메모리 ↑</li>
 *   <li>JPA flush 가 트랜잭션 commit 시점에 일괄 → 그 시점 전까지 INSERT SQL 안 나감</li>
 *   <li>{@code jdbc.batch_size=1000} 설정 (yml) 으로 commit 시점 일부 batching 은 발생</li>
 *   <li>R2 / R3 와 비교 baseline</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class SingleSaveStrategy implements BillingInsertStrategy {

    private final BillingRepository billingRepository;

    @Override
    public String name() { return "single-save"; }

    @Override
    @Transactional
    public int execute(List<Contract> contracts, Long batchLogId, String billingMonth) {
        int n = 0;
        for (Contract c : contracts) {
            billingRepository.save(BillingFactory.create(c, batchLogId, billingMonth));
            n++;
        }
        return n;
    }
}
