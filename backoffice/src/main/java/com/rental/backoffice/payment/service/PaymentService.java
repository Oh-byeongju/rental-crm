package com.rental.backoffice.payment.service;

import com.rental.domain.billing.entity.Billing;
import com.rental.domain.billing.repository.BillingRepository;
import com.rental.backoffice.billing.service.BillingService;
import com.rental.domain.common.exception.BusinessException;
import com.rental.domain.common.exception.ErrorCode;
import com.rental.domain.customer.entity.Customer;
import com.rental.domain.customer.repository.CustomerRepository;
import com.rental.backoffice.payment.dto.PaymentCancelRequest;
import com.rental.backoffice.payment.dto.PaymentCreateRequest;
import com.rental.backoffice.payment.dto.PaymentResponse;
import com.rental.backoffice.payment.dto.PaymentSearchRequest;
import com.rental.domain.payment.entity.Payment;
import com.rental.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 수납 도메인 — Ch.3 학습 핵심 (Kafka Producer/Consumer 멱등성).
 *
 * <p>책임:
 * <ul>
 *   <li>등록 (manual 수동): 청구 PAYABLE 검증 + 금액 일치 + 청구 PAID 전환 (BillingService 협력)</li>
 *   <li>취소 / 환불: 상태 전이 + 청구 상태 복원 — 정책 추후 (PAID → UNPAID/OVERDUE 자동 복원?)</li>
 *   <li>PAYMENT_NO 자동 채번 — `PAY-YYYYMM-NNNNNN`</li>
 *   <li>응답 매핑: 청구 + 고객 정보 (service-mapping-pattern.md)</li>
 * </ul>
 *
 * <p>⚠️ Ch.3 Kafka Producer 호출 (rental.payment.completed / rental.payment.cancelled) 는 후속 추가.
 * Toss Payments 결제 콜백 API 도 Phase 2 고객 포털 작업 시.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    /** PAYMENT_METHOD 정적 검증 — CM_CODE 그룹 PAYMENT_METHOD 와 일치. */
    private static final Set<String> VALID_METHODS = Set.of(
            Payment.METHOD_CARD, Payment.METHOD_BANK, Payment.METHOD_CASH, Payment.METHOD_TOSS);

    private static final DateTimeFormatter NO_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMM");

    private final PaymentRepository paymentRepository;
    private final BillingRepository billingRepository;
    private final BillingService billingService;
    private final CustomerRepository customerRepository;

    // ===================== Create =====================
    @Transactional
    public PaymentResponse register(PaymentCreateRequest req) {
        // 1. 청구 존재 + 수납 가능 상태
        Billing billing = billingRepository.findById(req.billingId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "청구 없음: " + req.billingId()));
        if (!Billing.PAYABLE_STATUS.contains(billing.getBillingStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "수납 불가 상태: " + billing.getBillingStatus());
        }

        // 2. 중복 수납 차단 (한 청구당 활성 수납 1건)
        long activeCount = paymentRepository.countActiveByBillingId(req.billingId());
        if (activeCount > 0) {
            throw new BusinessException(ErrorCode.ALREADY_EXISTS,
                    "이미 수납 완료된 청구: " + billing.getBillingNo());
        }

        // 3. 금액 일치 (분할 수납 X — 학습 단순화)
        if (!req.paymentAmount().equals(billing.getBillingAmount())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "수납금액 불일치 (분할 수납 미지원): 청구 "
                            + billing.getBillingAmount() + " / 수납 " + req.paymentAmount());
        }

        // 4. PAYMENT_METHOD / PAYMENT_DATE 검증
        if (!VALID_METHODS.contains(req.paymentMethod())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "유효하지 않은 수납 방법: " + req.paymentMethod());
        }
        if (req.paymentDate().isAfter(LocalDate.now())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "수납일은 오늘 이전만 가능: " + req.paymentDate());
        }

        // 5. Toss Payments 중복 차단 (METHOD=TOSS 시)
        if (Payment.METHOD_TOSS.equals(req.paymentMethod())
                && req.tossOrderId() != null
                && paymentRepository.existsByTossOrderId(req.tossOrderId())) {
            throw new BusinessException(ErrorCode.ALREADY_EXISTS,
                    "이미 처리된 Toss 주문번호: " + req.tossOrderId());
        }

        // 6. 임시 PAYMENT_NO 로 INSERT → 자동 채번 UPDATE
        Payment draft = Payment.builder()
                .paymentNo("TMP-" + System.nanoTime())
                .billingId(req.billingId())
                .customerId(billing.getCustomerId())
                .paymentAmount(req.paymentAmount())
                .paymentMethod(req.paymentMethod())
                .paymentDate(req.paymentDate())
                .tossOrderId(req.tossOrderId())
                .tossPaymentKey(req.tossPaymentKey())
                .build();
        Payment saved = paymentRepository.save(draft);
        saved.assignPaymentNo(generatePaymentNo(saved.getPaymentId()));

        // 7. 청구 PAID 전환 (BillingService 협력)
        billingService.markPaid(billing.getBillingId());

        // TODO: Ch.3 — Kafka Producer 발행 `rental.payment.completed` (멱등성 키: paymentNo)

        Customer customer = customerRepository.findById(billing.getCustomerId()).orElse(null);
        return PaymentResponse.from(saved, billing, customer);
    }

    // ===================== Read =====================

    public Page<PaymentResponse> search(PaymentSearchRequest req, Pageable pageable) {
        Page<Payment> page = paymentRepository.search(
                req.hasPaymentNo()     ? req.paymentNo()     : null,
                req.hasBillingId()     ? req.billingId()     : null,
                req.hasCustomerId()    ? req.customerId()    : null,
                req.hasPaymentMethod() ? req.paymentMethod() : null,
                req.hasPaymentStatus() ? req.paymentStatus() : null,
                req.hasDateFrom()      ? req.dateFrom()      : null,
                req.hasDateTo()        ? req.dateTo()        : null,
                pageable);

        Set<Long> billingIds  = page.getContent().stream().map(Payment::getBillingId).collect(Collectors.toSet());
        Set<Long> customerIds = page.getContent().stream().map(Payment::getCustomerId).collect(Collectors.toSet());
        Map<Long, Billing> billingMap = billingRepository.findAllById(billingIds).stream()
                .collect(Collectors.toMap(Billing::getBillingId, b -> b));
        Map<Long, Customer> customerMap = customerRepository.findAllById(customerIds).stream()
                .collect(Collectors.toMap(Customer::getCustomerId, c -> c));

        return page.map(p -> PaymentResponse.from(p,
                billingMap.get(p.getBillingId()),
                customerMap.get(p.getCustomerId())));
    }

    public PaymentResponse findById(Long paymentId) {
        return toResponse(loadPayment(paymentId));
    }

    // ===================== 상태 전이 =====================

    @Transactional
    public PaymentResponse cancel(Long paymentId, PaymentCancelRequest req) {
        Payment payment = loadPayment(paymentId);
        if (!Payment.CANCELLABLE_STATUS.contains(payment.getPaymentStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "취소 불가 상태: " + payment.getPaymentStatus());
        }
        payment.cancel(req.reason());
        // TODO: 청구 상태 복원 정책 결정 — PAID → UNPAID/OVERDUE 자동 vs 수동
        // TODO: Ch.3 — Kafka Producer 발행 `rental.payment.cancelled`
        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse refund(Long paymentId, PaymentCancelRequest req) {
        Payment payment = loadPayment(paymentId);
        if (!Payment.REFUNDABLE_STATUS.contains(payment.getPaymentStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "환불 불가 상태: " + payment.getPaymentStatus());
        }
        // TODO: Toss Payments 환불 API 호출 (METHOD=TOSS 시)
        payment.refund(req.reason());
        return toResponse(payment);
    }

    // ===================== Private =====================

    private Payment loadPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "수납 없음: " + paymentId));
    }

    /** `PAY-YYYYMM-NNNNNN` — 등록월 + paymentId 6자리 zero-padded. */
    private String generatePaymentNo(Long paymentId) {
        return String.format("PAY-%s-%06d", LocalDate.now().format(NO_DATE_FMT), paymentId);
    }

    private PaymentResponse toResponse(Payment payment) {
        Billing billing  = billingRepository.findById(payment.getBillingId()).orElse(null);
        Customer customer = customerRepository.findById(payment.getCustomerId()).orElse(null);
        return PaymentResponse.from(payment, billing, customer);
    }
}
