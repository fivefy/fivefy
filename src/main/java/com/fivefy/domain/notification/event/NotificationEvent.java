package com.fivefy.domain.notification.event;

import com.fivefy.domain.notification.enums.NotificationType;

public record NotificationEvent(
        Long targetUserId,
        NotificationType type,
        String content,
        Long actorId,    // 행위자 ID (팔로우한 유저, 좋아요 누른 유저 등) — 없으면 null
        Long resourceId  // 대상 리소스 ID (트랙 ID, 앨범 ID 등) — 없으면 null
) {
    public static NotificationEvent of(Long targetUserId, NotificationType type, String content) {
        return new NotificationEvent(targetUserId, type, content, null, null);
    }

    public static NotificationEvent of(Long targetUserId, NotificationType type, String content,
                                       Long actorId, Long resourceId) {
        return new NotificationEvent(targetUserId, type, content, actorId, resourceId);
    }
}
