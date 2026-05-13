package com.rental.backoffice.batch.controller;

import com.rental.backoffice.batch.service.BatchTriggerService;
import com.rental.domain.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 화면 → batch 호출 프록시 (ADR-014 Step 5).
 * <p>X-Internal-Token 은 backoffice 내부에서 보유 — 화면은 모름.
 */
@RestController
@RequestMapping("/api/admin/batch-trigger")
@RequiredArgsConstructor
public class BatchTriggerRestController {

    private final BatchTriggerService batchTriggerService;

    @PostMapping("/run/{scenario}")
    public ApiResponse<Map<String, String>> run(@PathVariable String scenario) {
        batchTriggerService.run(scenario);
        return ApiResponse.ok(Map.of("scenario", scenario), "배치 실행 요청 전송됨");
    }
}
