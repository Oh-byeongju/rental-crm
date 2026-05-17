package com.rental.backoffice.visit.service;

import com.rental.domain.common.exception.BusinessException;
import com.rental.domain.common.exception.ErrorCode;
import com.rental.domain.contract.entity.Contract;
import com.rental.domain.contract.repository.ContractRepository;
import com.rental.domain.engineer.entity.Engineer;
import com.rental.domain.engineer.repository.EngineerRepository;
import com.rental.backoffice.visit.dto.VisitCancelRequest;
import com.rental.backoffice.visit.dto.VisitCreateRequest;
import com.rental.backoffice.visit.dto.VisitResponse;
import com.rental.backoffice.visit.dto.VisitSearchRequest;
import com.rental.backoffice.visit.dto.VisitUpdateRequest;
import com.rental.domain.visit.entity.Visit;
import com.rental.domain.visit.event.VisitAssignedEvent;
import com.rental.domain.visit.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 방문 도메인 — 등록(배정) / 수정 / 완료 / 취소 + 기사별 5건 초과 검증.
 *
 * <p>책임:
 * <ul>
 *   <li>등록: 계약 ACTIVE + 기사 활성 + 예정일 미래 + VISIT_TYPE 검증 + 5건 초과 차단</li>
 *   <li>수정 (SCHEDULED 상태만): 기사 재배정 / 예정일 변경 / 메모 — 변경 시 5건 재검증</li>
 *   <li>상태 전이: complete (SCHEDULED→COMPLETED) / cancel (SCHEDULED→CANCELLED, reason 필수)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VisitService {

    private static final Set<String> VALID_TYPES = Set.of("INSTALL", "CHECK", "COLLECT");
    private static final long MAX_PER_DAY = 5;

    private final VisitRepository visitRepository;
    private final ContractRepository contractRepository;
    private final EngineerRepository engineerRepository;
    private final ApplicationEventPublisher events;

    // ===================== Create =====================
    @Transactional
    public VisitResponse register(VisitCreateRequest req) {
        Contract contract = contractRepository.findById(req.contractId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "계약 없음: " + req.contractId()));
        if (!Contract.STATUS_ACTIVE.equals(contract.getContractStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "ACTIVE 계약에만 방문 등록 가능: 현재 " + contract.getContractStatus());
        }
        Engineer engineer = engineerRepository.findById(req.engineerId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "기사 없음: " + req.engineerId()));
        if (!"Y".equals(engineer.getUseYn())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "비활성 기사: " + engineer.getEngineerCode());
        }
        if (!VALID_TYPES.contains(req.visitType())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "유효하지 않은 방문 유형: " + req.visitType() + " (INSTALL/CHECK/COLLECT)");
        }
        if (!req.scheduledDate().isAfter(LocalDate.now().minusDays(1))) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "방문 예정일은 오늘 이후만 가능: " + req.scheduledDate());
        }
        validateScheduleLimit(req.engineerId(), req.scheduledDate(), 0);

        Visit visit = Visit.builder()
                .contractId(req.contractId())
                .engineerId(req.engineerId())
                .visitType(req.visitType())
                .scheduledDate(req.scheduledDate())
                .memo(req.memo())
                .build();
        Visit saved = visitRepository.save(visit);

        // Ch.3 — tx commit 후 KafkaEventPublisher 가 rental.visit.assigned 전송
        // (kafka-event-contract.md §4 / 04 §0-3). Consumer: 알림 INSERT.
        events.publishEvent(new VisitAssignedEvent(
                saved.getVisitId(), saved.getEngineerId(), saved.getContractId()));

        return VisitResponse.from(saved, contract, engineer);
    }

    // ===================== Read =====================
    public Page<VisitResponse> search(VisitSearchRequest req, Pageable pageable) {
        Page<Visit> page = visitRepository.search(
                req.hasContractId()  ? req.contractId()  : null,
                req.hasEngineerId()  ? req.engineerId()  : null,
                req.hasVisitType()   ? req.visitType()   : null,
                req.hasVisitStatus() ? req.visitStatus() : null,
                req.hasDateFrom()    ? req.dateFrom()    : null,
                req.hasDateTo()      ? req.dateTo()      : null,
                pageable);

        Set<Long> contractIds = page.getContent().stream().map(Visit::getContractId).collect(Collectors.toSet());
        Set<Long> engineerIds = page.getContent().stream().map(Visit::getEngineerId).collect(Collectors.toSet());
        Map<Long, Contract> contractMap = contractRepository.findAllById(contractIds).stream()
                .collect(Collectors.toMap(Contract::getContractId, c -> c));
        Map<Long, Engineer> engineerMap = engineerRepository.findAllById(engineerIds).stream()
                .collect(Collectors.toMap(Engineer::getEngineerId, e -> e));

        return page.map(v -> VisitResponse.from(v,
                contractMap.get(v.getContractId()),
                engineerMap.get(v.getEngineerId())));
    }

    public VisitResponse findById(Long visitId) {
        return toResponse(loadVisit(visitId));
    }

    // ===================== Update (SCHEDULED 상태만) =====================
    @Transactional
    public VisitResponse update(Long visitId, VisitUpdateRequest req) {
        Visit visit = loadVisit(visitId);
        if (!Visit.STATUS_SCHEDULED.equals(visit.getVisitStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "SCHEDULED 상태에서만 수정 가능: 현재 " + visit.getVisitStatus());
        }
        Engineer engineer = engineerRepository.findById(req.engineerId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "기사 없음: " + req.engineerId()));
        if (!"Y".equals(engineer.getUseYn())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE, "비활성 기사: " + engineer.getEngineerCode());
        }
        if (!req.scheduledDate().isAfter(LocalDate.now().minusDays(1))) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "방문 예정일은 오늘 이후만 가능: " + req.scheduledDate());
        }

        // 5건 초과 재검증 — engineer/date 변경 시. 현 visit 가 같은 (engineer, date) 라면 본인 제외 1건.
        boolean engineerChanged = !visit.getEngineerId().equals(req.engineerId());
        boolean dateChanged     = !visit.getScheduledDate().equals(req.scheduledDate());
        if (engineerChanged || dateChanged) {
            long selfExclude = (!engineerChanged && !dateChanged) ? 1 : 0;
            validateScheduleLimit(req.engineerId(), req.scheduledDate(), selfExclude);
        }

        visit.changeEngineerId(req.engineerId());
        visit.changeScheduledDate(req.scheduledDate());
        visit.changeMemo(req.memo());
        return toResponse(visit);
    }

    // ===================== 상태 전이 =====================

    @Transactional
    public VisitResponse complete(Long visitId) {
        Visit visit = loadVisit(visitId);
        if (!Visit.COMPLETABLE_STATUS.contains(visit.getVisitStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "완료 불가 상태: " + visit.getVisitStatus());
        }
        visit.complete();
        return toResponse(visit);
    }

    @Transactional
    public VisitResponse cancel(Long visitId, VisitCancelRequest req) {
        Visit visit = loadVisit(visitId);
        if (!Visit.CANCELLABLE_STATUS.contains(visit.getVisitStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "취소 불가 상태: " + visit.getVisitStatus());
        }
        visit.cancel(req.reason());
        return toResponse(visit);
    }

    // ===================== Private =====================

    private Visit loadVisit(Long visitId) {
        return visitRepository.findById(visitId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "방문 없음: " + visitId));
    }

    /** 기사별 같은 날 일정 카운트 ≥ 5 차단. selfExclude = 1 이면 본인 제외. */
    private void validateScheduleLimit(Long engineerId, LocalDate date, long selfExclude) {
        long count = visitRepository.countByEngineerIdAndScheduledDate(engineerId, date) - selfExclude;
        if (count >= MAX_PER_DAY) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    String.format("기사별 일정 5건 초과 (%s — 현재 %d건)", date, count));
        }
    }

    private VisitResponse toResponse(Visit visit) {
        Contract contract = contractRepository.findById(visit.getContractId()).orElse(null);
        Engineer engineer = engineerRepository.findById(visit.getEngineerId()).orElse(null);
        return VisitResponse.from(visit, contract, engineer);
    }
}
