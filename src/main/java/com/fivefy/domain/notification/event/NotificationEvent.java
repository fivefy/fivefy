package com.fivefy.domain.notification.event;

import com.fivefy.domain.notification.enums.NotificationType;

public record NotificationEvent(
        Long targetUserId,
        NotificationType type,
        String content
) {
    public static NotificationEvent of(Long targetUserId, NotificationType type, String content) {
        return new NotificationEvent(targetUserId, type, content);
    }
}
