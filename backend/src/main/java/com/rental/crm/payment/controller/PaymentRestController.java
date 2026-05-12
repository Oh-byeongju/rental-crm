package com.rental.crm.payment.controller;

import com.rental.crm.common.response.ApiResponse;
import com.rental.crm.common.response.PageResponse;
import com.rental.crm.payment.dto.PaymentCancelRequest;
import com.rental.crm.payment.dto.PaymentCreateRequest;
import com.rental.crm.payment.dto.PaymentResponse;
import com.rental.crm.payment.dto.PaymentSearchRequest;
import com.rental.crm.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentRestController {

    private final PaymentService paymentService;

    @GetMapping
    public ApiResponse<PageResponse<PaymentResponse>> search(
            @ModelAttribute PaymentSearchRequest search,
            @PageableDefault(size = 20, sort = "paymentId", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(paymentService.search(search, pageable)));
    }

    @GetMapping("/{paymentId}")
    public ApiResponse<PaymentResponse> detail(@PathVariable Long paymentId) {
        return ApiResponse.ok(paymentService.findById(paymentId));
    }

    @PostMapping
    public ApiResponse<PaymentResponse> register(@Valid @RequestBody PaymentCreateRequest req) {
        return ApiResponse.ok(paymentService.register(req), "수납이 등록되었습니다");
    }

    @PutMapping("/{paymentId}/cancel")
    public ApiResponse<PaymentResponse> cancel(@PathVariable Long paymentId,
                                               @Valid @RequestBody PaymentCancelRequest req) {
        return ApiResponse.ok(paymentService.cancel(paymentId, req), "수납이 취소되었습니다");
    }

    @PutMapping("/{paymentId}/refund")
    public ApiResponse<PaymentResponse> refund(@PathVariable Long paymentId,
                                               @Valid @RequestBody PaymentCancelRequest req) {
        return ApiResponse.ok(paymentService.refund(paymentId, req), "환불 처리되었습니다");
    }
}
