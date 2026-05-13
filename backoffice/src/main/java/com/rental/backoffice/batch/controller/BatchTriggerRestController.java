package com.rental.backoffice.batch.controller;

import com.rental.backoffice.batch.dto.BatchTriggerRequest;
import com.rental.backoffice.batch.service.BatchTriggerService;
import com.rental.domain.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/batch-trigger")
@RequiredArgsConstructor
public class BatchTriggerRestController {

    private final BatchTriggerService batchTriggerService;

    @PostMapping("/run/{scenario}")
    public ApiResponse<Map<String, String>> run(@PathVariable String scenario,
                                                @RequestBody(required = false) BatchTriggerRequest req) {
        batchTriggerService.run(scenario, req);
        return ApiResponse.ok(Map.of("scenario", scenario), "배치 실행 요청 전송됨");
    }
}
