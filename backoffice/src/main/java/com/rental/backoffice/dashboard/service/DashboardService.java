package com.rental.backoffice.dashboard.service;

import com.rental.backoffice.dashboard.dto.DashboardSummary;
import com.rental.domain.billing.entity.Billing;
import com.rental.domain.billing.repository.BillingRepository;
import com.rental.domain.contract.entity.Contract;
import com.rental.domain.contract.repository.ContractRepository;
import com.rental.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.YearMonth;
import java.util.List;

/**
 * 대시보드 집계 — 04 기능명세 §9 / 07 API 명세 §12-1.
 *
 * <p>캐시 정책: Redis cache-aside. {@code dashboard:summary} 키, TTL 5분.
 * HIT 즉시 반환 / MISS DB 집계 후 저장. {@link #refresh()} 가 {@code @Scheduled} 로
 * 주기 워밍 → 사용자 요청이 콜드 집계를 만나는 빈도 ↓.
 *
 * <p>집계 4종(계약/청구/미납/수납)은 각 Repository count·sum 으로 위임 — 풀스캔 없이
 * 인덱스/카운트. 단일 스냅샷 일관성은 대시보드 특성상 불필요해 클래스 tx 미적용
 * (Repository 호출별 tx).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final String CACHE_KEY = "dashboard:summary";
    private static final Duration TTL = Duration.ofMinutes(5);
    private static final List<String> UNPAID_STATUSES =
            List.of(Billing.STATUS_UNPAID, Billing.STATUS_OVERDUE);

    private final ContractRepository contractRepository;
    private final BillingRepository billingRepository;
    private final PaymentRepository paymentRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    /** 캐시 우선 조회 (화면/ API 공통 진입점). */
    public DashboardSummary getSummary() {
        Object cached = redisTemplate.opsForValue().get(CACHE_KEY);
        if (cached instanceof DashboardSummary ds) {
            return ds;
        }
        DashboardSummary fresh = aggregate();
        redisTemplate.opsForValue().set(CACHE_KEY, fresh, TTL);
        return fresh;
    }

    /** 주기 워밍 — 캐시를 항상 데워 둠 (cache-aside 콜드 빈도 ↓). */
    @Scheduled(initialDelay = 30_000, fixedRate = 300_000)
    public void refresh() {
        DashboardSummary fresh = aggregate();
        redisTemplate.opsForValue().set(CACHE_KEY, fresh, TTL);
        log.debug("[dashboard] cache refreshed {}", fresh);
    }

    private DashboardSummary aggregate() {
        YearMonth ym = YearMonth.now();
        var summary = new DashboardSummary(
                contractRepository.countByContractStatus(Contract.STATUS_ACTIVE),
                billingRepository.countByBillingMonth(ym.toString()),               // "YYYY-MM"
                billingRepository.countByBillingStatusIn(UNPAID_STATUSES),
                billingRepository.sumAmountByBillingStatusIn(UNPAID_STATUSES),
                paymentRepository.sumCompletedAmountBetween(ym.atDay(1), ym.atEndOfMonth())
        );
        log.info("[dashboard] aggregated {}", summary);
        return summary;
    }
}
