package com.rental.batch.billing.strategy;

import com.rental.batch.billing.BillingFactory;
import com.rental.domain.billing.repository.BillingRepository;
import com.rental.domain.contract.entity.Contract;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * R6-B — 멱등성: SELECT 후 INSERT (애플리케이션 측 per-row 가드).
 *
 * <p>각 계약마다 {@link BillingRepository#existsByContractIdAndBillingMonth} 로 존재 여부를
 * 먼저 조회 → 없을 때만 INSERT. 제약 위반이 DB 까지 가지 않으므로 rollback-only 함정도 없다.
 *
 * <p>측정 포인트: <b>5만 건 추가 SELECT</b> 비용(전부 중복인 재실행 = exists 5만 회 + INSERT 0).
 * R6-A 가 "일단 넣고 터지면 잡기" 라면 B 는 "넣기 전에 매번 확인" — 라운드트립 5만 회는 동일하나
 * 예외 경로 대신 인덱스 조회 경로({@code UK_BL_BILLING_CONTRACT_MONTH} unique index) 비용.
 *
 * <p>full-overlap 측정에서는 save() 가 0회라 영속성 컨텍스트가 비어 있어 1차 캐시 노이즈 없음.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SelectThenInsertStrategy implements BillingInsertStrategy {

    private final BillingRepository billingRepository;

    @Override
    public String name() { return "select-insert"; }

    @Override
    @Transactional
    public int execute(List<Contract> contracts, Long batchLogId, String billingMonth) {
        int inserted = 0;
        int skipped  = 0;
        for (Contract c : contracts) {
            if (billingRepository.existsByContractIdAndBillingMonth(c.getContractId(), billingMonth)) {
                skipped++;   // 이미 존재 — 멱등 no-op
            } else {
                billingRepository.save(BillingFactory.create(c, batchLogId, billingMonth));
                inserted++;
            }
        }
        log.info("[R6-B select-insert] inserted={} skipped={} total={}",
                 inserted, skipped, contracts.size());
        return inserted + skipped;   // 멱등 성공 = 원하는 상태 도달 건수
    }
}
