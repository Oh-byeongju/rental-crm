package com.rental.crm.billing.controller;

import com.rental.crm.billing.dto.BillingResponse;
import com.rental.crm.billing.dto.BillingSearchRequest;
import com.rental.crm.billing.service.BillingService;
import com.rental.crm.common.response.ApiResponse;
import com.rental.crm.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 청구 REST API — 조회 + 단건 상태 전이.
 * 배치 트리거 (월 청구 일괄 생성) 는 별도 추가 예정 (Ch.1 컨테이너 검증 후).
 */
@RestController
@RequestMapping("/api/billings")
@RequiredArgsConstructor
public class BillingRestController {

    private final BillingService billingService;

    @GetMapping
    public ApiResponse<PageResponse<BillingResponse>> search(
            @ModelAttribute BillingSearchRequest search,
            @PageableDefault(size = 20, sort = "billingId") Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(billingService.search(search, pageable)));
    }

    @GetMapping("/{billingId}")
    public ApiResponse<BillingResponse> detail(@PathVariable Long billingId) {
        return ApiResponse.ok(billingService.findById(billingId));
    }

    @PutMapping("/{billingId}/due-date")
    public ApiResponse<BillingResponse> changeDueDate(
            @PathVariable Long billingId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate) {
        return ApiResponse.ok(billingService.changeDueDate(billingId, dueDate), "납기일이 변경되었습니다");
    }

    @PutMapping("/{billingId}/cancel")
    public ApiResponse<BillingResponse> cancel(@PathVariable Long billingId) {
        return ApiResponse.ok(billingService.cancel(billingId), "청구가 취소되었습니다");
    }
}
