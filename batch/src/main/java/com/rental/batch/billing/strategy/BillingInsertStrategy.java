package com.rental.batch.billing.strategy;

import com.rental.domain.contract.entity.Contract;

import java.util.List;

/**
 * Ch.1 청구 INSERT 전략. 각 라운드별 구현체.
 *
 * <p>{@link #name()} = `BATCH_PARAMS` JSON 의 strategy 값. UI/요청 인자로 식별.
 */
public interface BillingInsertStrategy {

    String name();

    /**
     * 활성 계약 리스트로 청구 INSERT.
     * @return 성공 INSERT 건수
     */
    int execute(List<Contract> contracts, Long batchLogId, String billingMonth);
}
