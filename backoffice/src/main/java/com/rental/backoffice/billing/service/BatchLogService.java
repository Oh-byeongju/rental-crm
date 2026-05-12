package com.rental.backoffice.billing.service;

import com.rental.backoffice.billing.dto.BatchLogResponse;
import com.rental.backoffice.billing.dto.BatchLogSearchRequest;
import com.rental.domain.billing.entity.BatchLog;
import com.rental.domain.billing.repository.BatchLogRepository;
import com.rental.domain.common.exception.BusinessException;
import com.rental.domain.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 배치 실행 이력 도메인 — 조회 전용 (등록 / 갱신은 배치 Service 가 직접).
 *
 * <p>책임:
 * <ul>
 *   <li>search / findById — 배치 이력 조회 화면용</li>
 *   <li>Ch.1 핵심: DURATION_MS 추이 분석 (건별 INSERT vs JDBC bulk INSERT 성능 비교)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BatchLogService {

    private final BatchLogRepository batchLogRepository;

    public Page<BatchLogResponse> search(BatchLogSearchRequest req, Pageable pageable) {
        return batchLogRepository.search(
                req.hasBatchType()   ? req.batchType()   : null,
                req.hasBatchStatus() ? req.batchStatus() : null,
                pageable
        ).map(BatchLogResponse::from);
    }

    public BatchLogResponse findById(Long batchLogId) {
        BatchLog log = batchLogRepository.findById(batchLogId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "배치 이력 없음: " + batchLogId));
        return BatchLogResponse.from(log);
    }
}
