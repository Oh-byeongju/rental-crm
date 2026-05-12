package com.rental.crm.notification.service;

import com.rental.crm.common.exception.BusinessException;
import com.rental.crm.common.exception.ErrorCode;
import com.rental.crm.notification.dto.NotificationResponse;
import com.rental.crm.notification.dto.NotificationSearchRequest;
import com.rental.crm.notification.entity.Notification;
import com.rental.crm.notification.repository.NotificationRepository;
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
 *   <li>publish — Kafka Consumer 가 호출하여 알림 INSERT (Ch.3 작업 시)</li>
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

    // ===================== Create (Kafka Consumer 가 호출) =====================

    /**
     * 알림 발행 — Ch.3 Kafka Consumer 에서 토픽 수신 후 호출.
     *
     * @param recipientUserId NULL = 전체 broadcast
     * @param refType / refId 참조 도메인 (예: BILLING / 123) — 알림 클릭 시 상세 페이지 이동용
     */
    @Transactional
    public NotificationResponse publish(Long recipientUserId, String notificationType, String message,
                                        String refType, Long refId) {
        Notification notification = Notification.builder()
                .recipientUserId(recipientUserId)
                .notificationType(notificationType)
                .message(message)
                .refType(refType)
                .refId(refId)
                .build();
        return NotificationResponse.from(notificationRepository.save(notification));
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
