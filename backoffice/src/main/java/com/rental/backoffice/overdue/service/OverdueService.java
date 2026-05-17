package com.rental.backoffice.overdue.service;

import com.rental.domain.billing.entity.Billing;
import com.rental.domain.billing.repository.BillingRepository;
import com.rental.domain.common.exception.BusinessException;
import com.rental.domain.common.exception.ErrorCode;
import com.rental.domain.customer.entity.Customer;
import com.rental.domain.customer.repository.CustomerRepository;
import com.rental.backoffice.overdue.dto.OverdueResolveRequest;
import com.rental.backoffice.overdue.dto.OverdueResponse;
import com.rental.backoffice.overdue.dto.OverdueSearchRequest;
import com.rental.domain.overdue.entity.Overdue;
import com.rental.domain.overdue.repository.OverdueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 연체 도메인 — 조회 + 수동 해제 + 배치 진입점 (Ch.3 OVERDUE_UPDATE 보류).
 *
 * <p>책임:
 * <ul>
 *   <li>search / findById — 페이지 + 단건 ({@code service-mapping-pattern.md} 적용)</li>
 *   <li>resolveManually — 운영자 수동 해제 (reason 필수)</li>
 *   <li>upsert (배치 트리거) — UNPAID + DUE_DATE 지남 인 청구에 대해 INSERT 또는 reopen (Ch.3 작업 시)</li>
 *   <li>resolveByPayment — 수납 완료 시 자동 해제 트리거 (Ch.3 작업 시 PaymentService 연동)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OverdueService {

    private final OverdueRepository overdueRepository;
    private final BillingRepository billingRepository;
    private final CustomerRepository customerRepository;

    // ===================== Read =====================

    public Page<OverdueResponse> search(OverdueSearchRequest req, Pageable pageable) {
        Page<Overdue> page = overdueRepository.search(
                req.hasCustomerId()   ? req.customerId()   : null,
                req.hasBillingId()    ? req.billingId()    : null,
                req.hasResolvedOnly() ? req.resolvedOnly() : null,
                req.hasMinDays()      ? req.minDays()      : null,
                pageable);

        Set<Long> billingIds  = page.getContent().stream().map(Overdue::getBillingId).collect(Collectors.toSet());
        Set<Long> customerIds = page.getContent().stream().map(Overdue::getCustomerId).collect(Collectors.toSet());
        Map<Long, Billing> billingMap = billingRepository.findAllById(billingIds).stream()
                .collect(Collectors.toMap(Billing::getBillingId, b -> b));
        Map<Long, Customer> customerMap = customerRepository.findAllById(customerIds).stream()
                .collect(Collectors.toMap(Customer::getCustomerId, c -> c));

        return page.map(o -> OverdueResponse.from(o,
                billingMap.get(o.getBillingId()),
                customerMap.get(o.getCustomerId())));
    }

    public OverdueResponse findById(Long overdueId) {
        return toResponse(loadOverdue(overdueId));
    }

    // ===================== 해제 (수동) =====================

    @Transactional
    public OverdueResponse resolveManually(Long overdueId, OverdueResolveRequest req) {
        Overdue overdue = loadOverdue(overdueId);
        if (overdue.isResolved()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "이미 해결된 연체: " + overdueId);
        }
        overdue.resolve(req.reason());
        return toResponse(overdue);
    }

    // ===================== Ch.3 — 수납 완료 자동 해제 (Kafka Consumer) =====================

    /**
     * 수납 완료 시 연체 자동 해제 — {@code rental.payment.completed} Consumer 가 호출.
     *
     * <p>자연 멱등: 연체 레코드 없음(애초에 미연체) / 이미 해결 → no-op. Kafka 재전송 안전
     * (kafka-event-contract.md §5, api-safety §2-1 상태 재조회). 변경분은 dirty checking 으로 flush.
     */
    @Transactional
    public void resolveByPayment(Long billingId) {
        overdueRepository.findByBillingId(billingId).ifPresent(o -> {
            if (!o.isResolved()) {
                o.resolve("수납 완료 자동 해제 (Kafka rental.payment.completed)");
            }
        });
    }

    // ===================== 배치 진입점 (OVERDUE_UPDATE — 별도 시나리오, 미구현) =====================

    // TODO: upsert(billingId, customerId, amount, days) — OVERDUE_UPDATE 배치 진입점
    //   - findByBillingId 후 존재하면 updateDays / reopen, 없으면 INSERT

    // ===================== Private =====================

    private Overdue loadOverdue(Long overdueId) {
        return overdueRepository.findById(overdueId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "연체 없음: " + overdueId));
    }

    private OverdueResponse toResponse(Overdue overdue) {
        Billing billing  = billingRepository.findById(overdue.getBillingId()).orElse(null);
        Customer customer = customerRepository.findById(overdue.getCustomerId()).orElse(null);
        return OverdueResponse.from(overdue, billing, customer);
    }
}
