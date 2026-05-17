package com.rental.backoffice.common.kafka;

import com.rental.backoffice.notification.service.NotificationService;
import com.rental.backoffice.overdue.service.OverdueService;
import com.rental.domain.billing.entity.Billing;
import com.rental.domain.billing.event.BillingCreatedEvent;
import com.rental.domain.billing.repository.BillingRepository;
import com.rental.domain.common.kafka.KafkaTopics;
import com.rental.domain.contract.entity.Contract;
import com.rental.domain.contract.repository.ContractRepository;
import com.rental.domain.customer.entity.Customer;
import com.rental.domain.customer.repository.CustomerRepository;
import com.rental.domain.engineer.entity.Engineer;
import com.rental.domain.engineer.repository.EngineerRepository;
import com.rental.domain.notification.entity.Notification;
import com.rental.domain.payment.event.PaymentCompletedEvent;
import com.rental.domain.visit.event.VisitAssignedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka Consumer (backoffice 소비분) — 04 §0-3 / §10-1, 계약 룰 kafka-event-contract.md §5.
 *
 * <p>offset 정책: manual ack (`enable-auto-commit:false`, `ack-mode:manual_immediate`).
 * 처리 <b>성공 후에만</b> {@code ack.acknowledge()}. 실패 시 ack 안 함 → 재전송.
 * 재전송 안전성은 처리 효과의 <b>자연 멱등</b>으로 보장:
 * {@code resolveByPayment}=no-op-on-repeat, {@code publishIfAbsent}=existsBy 가드.
 *
 * <p>billing.created 는 batch 가 발행, 여기서 소비. payment.overdue 는 producer(OVERDUE_UPDATE
 * 배치) 미구현이라 listener 없음(speculative 회피 — 그 시나리오 작업 시 producer+consumer 동반).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventConsumer {

    private final NotificationService notificationService;
    private final OverdueService overdueService;
    private final CustomerRepository customerRepository;
    private final BillingRepository billingRepository;
    private final ContractRepository contractRepository;
    private final EngineerRepository engineerRepository;

    /** 수납 완료 → 연체 자동해제 + 알림 (04 §7-1). */
    @KafkaListener(topics = KafkaTopics.PAYMENT_COMPLETED)
    public void onPaymentCompleted(PaymentCompletedEvent e, Acknowledgment ack) {
        handle(KafkaTopics.PAYMENT_COMPLETED, e, ack, () -> {
            overdueService.resolveByPayment(e.billingId());
            String customer = customerRepository.findById(e.customerId())
                    .map(Customer::getCustomerName).orElse("고객#" + e.customerId());
            String month = billingRepository.findById(e.billingId())
                    .map(Billing::getBillingMonth).orElse("");
            notificationService.publishIfAbsent(null, Notification.TYPE_PAYMENT_COMPLETED,
                    "%s 님의 %s 수납이 완료되었습니다.".formatted(customer, month),
                    "BILLING", e.billingId());
        });
    }

    /** 방문 배정 완료 → 알림 (04 §0-3). */
    @KafkaListener(topics = KafkaTopics.VISIT_ASSIGNED)
    public void onVisitAssigned(VisitAssignedEvent e, Acknowledgment ack) {
        handle(KafkaTopics.VISIT_ASSIGNED, e, ack, () -> {
            String contractNo = contractRepository.findById(e.contractId())
                    .map(Contract::getContractNo).orElse("계약#" + e.contractId());
            String engineer = engineerRepository.findById(e.engineerId())
                    .map(Engineer::getEngineerName).orElse("기사#" + e.engineerId());
            notificationService.publishIfAbsent(null, Notification.TYPE_VISIT_ASSIGNED,
                    "%s 방문이 %s에게 배정되었습니다.".formatted(contractNo, engineer),
                    "VISIT", e.visitId());
        });
    }

    /** 월 청구 배치 완료 → 알림 (04 §8 / §10-1). 멱등키 refId = YYYYMM. */
    @KafkaListener(topics = KafkaTopics.BILLING_CREATED)
    public void onBillingCreated(BillingCreatedEvent e, Acknowledgment ack) {
        handle(KafkaTopics.BILLING_CREATED, e, ack, () ->
                notificationService.publishIfAbsent(null, Notification.TYPE_BILLING_CREATED,
                        "%s 청구서 %d건이 생성되었습니다.".formatted(e.billingMonth(), e.count()),
                        "BILLING_MONTH", Long.parseLong(e.billingMonth().replace("-", ""))));
    }

    /** recv 로깅 → 처리 → 성공 시에만 ack. 실패 시 ack 안 함(재전송) + rethrow. */
    private void handle(String topic, Object event, Acknowledgment ack, Runnable work) {
        log.info("[kafka-con] recv topic={} payload={}", topic, event);
        try {
            work.run();
            ack.acknowledge();
            log.info("[kafka-con] done topic={}", topic);
        } catch (RuntimeException ex) {
            log.error("[kafka-con] FAILED topic={} payload={} — no ack, redeliver", topic, event, ex);
            throw ex;
        }
    }
}
