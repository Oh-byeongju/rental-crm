package com.rental.backoffice.notification.service;

import com.rental.domain.common.exception.BusinessException;
import com.rental.domain.common.exception.ErrorCode;
import com.rental.backoffice.notification.dto.NotificationResponse;
import com.rental.backoffice.notification.dto.NotificationSearchRequest;
import com.rental.domain.notification.entity.Notification;
import com.rental.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 도메인 — 조회 + 읽음 처리 + 등록 (Kafka Consumer 호출).
 *
 * <p>책임:
 * <ul>
 *   <li>publishIfAbsent — Kafka Consumer 가 호출, (유형,참조)중복 시 skip (멱등 INSERT)</li>
 *   <li>search — 사용자별 알림 목록 (broadcast 포함)</li>
 *   <li>markRead / markAllRead — 단건/전체 읽음 처리</li>
 *   <li>countUnread — 헤더 뱃지 표시용</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // ===================== Create (Kafka Consumer 가 호출 — 멱등) =====================

    /**
     * 알림 멱등 발행 — Ch.3 Kafka Consumer 가 토픽 수신 후 호출.
     *
     * <p>동일 {@code (notificationType, refType, refId)} 알림이 이미 있으면 skip — Kafka
     * 재전송(at-least-once) 안전. 가드 SELECT + INSERT 는 본 메서드의 동일 tx 안에서 수행해야
     * commit 된 행이 보임 (Oracle READ COMMITTED — api-safety §2-3/§5, kafka-event-contract §5).
     *
     * @param recipientUserId NULL = 전체 broadcast
     * @param refType / refId 참조 도메인 (예: BILLING / 123) — 알림 클릭 시 상세 페이지 이동 + 멱등키
     */
    @Transactional
    public void publishIfAbsent(Long recipientUserId, String notificationType, String message,
                                String refType, Long refId) {
        if (notificationRepository.existsByNotificationTypeAndRefTypeAndRefId(
                notificationType, refType, refId)) {
            return;   // 이미 처리됨 — 재전송 skip
        }
        notificationRepository.save(Notification.builder()
                .recipientUserId(recipientUserId)
                .notificationType(notificationType)
                .message(message)
                .refType(refType)
                .refId(refId)
                .build());
    }

    // ===================== Read =====================

    public Page<NotificationResponse> findByUserId(Long userId, NotificationSearchRequest req, Pageable pageable) {
        return notificationRepository.findByUserId(
                userId,
                req.hasReadYn()           ? req.readYn()           : null,
                req.hasNotificationType() ? req.notificationType() : null,
                pageable
        ).map(NotificationResponse::from);
    }

    public long countUnread(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    // ===================== Update (읽음 처리) =====================

    @Transactional
    public NotificationResponse markRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "알림 없음: " + notificationId));

        // 소유권 검증 (broadcast 제외) — api-safety.md §2-4
        if (notification.getRecipientUserId() != null
                && !notification.getRecipientUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인 알림만 읽음 처리 가능");
        }
        notification.markRead();
        return NotificationResponse.from(notification);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepository.markAllReadByUserId(userId);
    }
}
