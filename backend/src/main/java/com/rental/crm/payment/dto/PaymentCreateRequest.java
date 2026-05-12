package com.rental.crm.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

import java.time.LocalDate;

/**
 * 수납 등록 — 수동 (관리자가 카드/계좌이체/현금 직접 등록).
 * Toss Payments 결제는 별도 콜백 API 시점에 처리 (Phase 2 고객 포털).
 *
 * <p>분할 수납 X — 청구 금액과 paymentAmount 일치 필수 (학습 단순화).
 */
public record PaymentCreateRequest(
        @NotNull
        Long billingId,

        @NotNull @Positive
        Long paymentAmount,

        @NotBlank @Size(max = 20)
        String paymentMethod,

        @NotNull
        LocalDate paymentDate,

        @Nullable @Size(max = 200)
        String tossOrderId,

        @Nullable @Size(max = 200)
        String tossPaymentKey
) {}
