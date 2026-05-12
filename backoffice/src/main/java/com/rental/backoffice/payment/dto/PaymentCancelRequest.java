package com.rental.backoffice.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 수납 취소 / 환불 공용 — reason 필수.
 * 취소: 등록 자체를 무효화. 환불: Toss API 호출 후 REFUNDED.
 */
public record PaymentCancelRequest(
        @NotBlank @Size(max = 1000)
        String reason
) {}
