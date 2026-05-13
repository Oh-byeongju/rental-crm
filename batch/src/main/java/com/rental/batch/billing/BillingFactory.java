package com.rental.batch.billing;

import com.rental.domain.billing.entity.Billing;
import com.rental.domain.contract.entity.Contract;

import java.time.LocalDate;

/**
 * 청구 객체 생성 (Ch.1 측정용).
 *
 * <p>운영 자동 채번 (ADR-011 — 임시값 INSERT 후 UPDATE 패턴) 과 별개.
 * 본 팩토리는 BILLING_NO = `BL-{YYYYMM}-{CONTRACT_ID:010d}` 로 INSERT 시점 직접 박음.
 * UNIQUE (CONTRACT_ID, BILLING_MONTH) 와 자연 일치 → 추가 UPDATE 없음 = 측정 노이즈 ↓.
 */
public final class BillingFactory {

    private BillingFactory() {}

    public static Billing create(Contract c, Long batchLogId, String billingMonth) {
        var yyyymm = billingMonth.replace("-", "");
        return Billing.builder()
                .billingNo("BL-" + yyyymm + "-" + String.format("%010d", c.getContractId()))
                .contractId(c.getContractId())
                .customerId(c.getCustomerId())
                .batchLogId(batchLogId)
                .billingMonth(billingMonth)
                .billingAmount(c.getMonthlyFee())
                .issueDate(LocalDate.parse(billingMonth + "-01"))
                .dueDate(LocalDate.parse(billingMonth + "-25"))
                .build();
    }
}
