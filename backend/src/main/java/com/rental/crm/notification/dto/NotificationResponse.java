package com.rental.crm.notification.dto;

import com.rental.crm.notification.entity.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long notificationId,
        Long recipientUserId,
        String notificationType,
        String message,
        String refType,
        Long refId,
        String readYn,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getNotificationId(),
                n.getRecipientUserId(),
                n.getNotificationType(),
                n.getMessage(),
                n.getRefType(),
                n.getRefId(),
                n.getReadYn(),
                n.getFirsRegDts()
        );
    }
}
