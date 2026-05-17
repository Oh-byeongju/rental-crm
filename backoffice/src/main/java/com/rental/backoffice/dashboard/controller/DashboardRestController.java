package com.rental.backoffice.dashboard.controller;

import com.rental.backoffice.dashboard.dto.DashboardSummary;
import com.rental.backoffice.dashboard.service.DashboardService;
import com.rental.domain.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 대시보드 API — 07 API 명세 §12-1. Redis 캐시 우선 (DashboardService).
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardRestController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ApiResponse<DashboardSummary> summary() {
        return ApiResponse.ok(dashboardService.getSummary());
    }
}
