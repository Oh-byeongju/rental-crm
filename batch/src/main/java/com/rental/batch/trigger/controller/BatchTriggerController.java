package com.rental.batch.trigger.controller;

import com.rental.batch.trigger.dto.BatchRunRequest;
import com.rental.batch.trigger.service.BatchRunnerService;
import com.rental.domain.billing.entity.BatchLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

/**
 * 내부 호출 진입점 (ADR-014 §통신 — REST fire-and-forget).
 * <p>{@code X-Internal-Token} 검증은 {@code InternalTokenFilter} 가 수행.
 */
@RestController
@RequestMapping("/internal/batch")
@RequiredArgsConstructor
public class BatchTriggerController {

    private static final Set<String> ALLOWED = Set.of(
            BatchLog.TYPE_DUMMY_SUCCESS,
            BatchLog.TYPE_DUMMY_FAIL,
            BatchLog.TYPE_BILLING_CREATE,
            BatchLog.TYPE_ENERGY_CUSTOMER_SYNC
    );

    private final BatchRunnerService runner;

    @PostMapping("/run/{scenario}")
    public ResponseEntity<Map<String, String>> run(@PathVariable String scenario,
                                                   @RequestBody(required = false) BatchRunRequest req) {
        if (!ALLOWED.contains(scenario)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "unknown scenario",
                    "scenario", scenario
            ));
        }
        runner.run(scenario, req != null ? req : BatchRunRequest.empty());
        return ResponseEntity.accepted().body(Map.of(
                "scenario", scenario,
                "status", "ACCEPTED"
        ));
    }
}
