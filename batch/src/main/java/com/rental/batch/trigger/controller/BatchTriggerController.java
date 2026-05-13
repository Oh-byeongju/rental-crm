package com.rental.batch.trigger.controller;

import com.rental.batch.trigger.service.BatchRunnerService;
import com.rental.domain.billing.entity.BatchLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

/**
 * 내부 호출 진입점 (ADR-014 §통신 — REST fire-and-forget).
 * <p>{@code X-Internal-Token} 검증은 {@code InternalTokenFilter} 가 수행 (경로 prefix 기반).
 */
@RestController
@RequestMapping("/internal/batch")
@RequiredArgsConstructor
public class BatchTriggerController {

    /** Step 5 = 더미만 허용. Step 7 부터 실 시나리오 추가 시 갱신. */
    private static final Set<String> ALLOWED = Set.of(
            BatchLog.TYPE_DUMMY_SUCCESS,
            BatchLog.TYPE_DUMMY_FAIL
    );

    private final BatchRunnerService runner;

    @PostMapping("/run/{scenario}")
    public ResponseEntity<Map<String, String>> run(@PathVariable String scenario) {
        if (!ALLOWED.contains(scenario)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "unknown scenario",
                    "scenario", scenario
            ));
        }
        runner.run(scenario);   // 즉시 반환, 실제 작업은 별 스레드
        return ResponseEntity.accepted().body(Map.of(
                "scenario", scenario,
                "status", "ACCEPTED"
        ));
    }
}
