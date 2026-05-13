package com.rental.backoffice.batch.service;

import com.rental.backoffice.batch.dto.BatchTriggerRequest;
import com.rental.domain.common.exception.BusinessException;
import com.rental.domain.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * batch 모듈 호출 (ADR-014 §통신 — REST fire-and-forget).
 * <p>호출 자체는 동기지만 batch 의 Controller 가 즉시 202 반환하므로 사용자 체감은 비동기.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchTriggerService {

    private final RestClient batchRestClient;

    public void run(String scenario, BatchTriggerRequest req) {
        try {
            Map<?, ?> body = batchRestClient.post()
                    .uri("/internal/batch/run/{scenario}", scenario)
                    .body(req != null ? req : BatchTriggerRequest.empty())
                    .retrieve()
                    .body(Map.class);
            log.info("[batch-trigger] scenario={} req={} ack={}", scenario, req, body);
        } catch (RestClientException e) {
            log.error("[batch-trigger] scenario={} failed", scenario, e);
            throw new BusinessException(ErrorCode.EXTERNAL_SYSTEM_ERROR,
                    "배치 호출에 실패했습니다: " + e.getMessage());
        }
    }
}
