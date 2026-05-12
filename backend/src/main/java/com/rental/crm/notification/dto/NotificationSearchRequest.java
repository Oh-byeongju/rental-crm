package com.rental.crm.notification.dto;

import org.springframework.lang.Nullable;

public record NotificationSearchRequest(
        @Nullable String readYn,
        @Nullable String notificationType
) {
    public boolean hasReadYn()           { return readYn           != null && !readYn.isBlank(); }
    public boolean hasNotificationType() { return notificationType != null && !notificationType.isBlank(); }
}
