package com.fivefy.domain.notification.dto.response;

import com.fivefy.domain.notification.entity.Notification;
import com.fivefy.domain.notification.enums.NotificationChannel;
import com.fivefy.domain.notification.enums.NotificationStatus;
import com.fivefy.domain.notification.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationGetResponse (

        Long id,
        NotificationType type,
        String content,
        NotificationStatus status,
        NotificationChannel channel,
        LocalDateTime readAt,
        LocalDateTime createdAt
){
    public static NotificationGetResponse from(Notification notification){
        return new NotificationGetResponse(
                notification.getId(),
                notification.getType(),
                notification.getContent(),
                notification.getStatus(),
                notification.getChannel(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
