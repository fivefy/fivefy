package com.fivefy.domain.notification.dto;

import com.fivefy.domain.notification.enums.NotificationChannel;
import com.fivefy.domain.notification.enums.NotificationStatus;
import com.fivefy.domain.notification.enums.NotificationType;

import java.time.LocalDateTime;

/**
 * Redis Pub/Sub 메시지 페이로드
 * JSON 직렬화를 위해 record 대신 일반 클래스 사용
 */
public class NotificationMessage {

    private Long notificationId;
    private Long userId;
    private NotificationType type;
    private String content;
    private NotificationStatus status;
    private NotificationChannel channel;
    private LocalDateTime createdAt;

    public NotificationMessage() {}

    public NotificationMessage(Long notificationId, Long userId, NotificationType type,
                                String content, NotificationStatus status,
                                NotificationChannel channel, LocalDateTime createdAt) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.type = type;
        this.content = content;
        this.status = status;
        this.channel = channel;
        this.createdAt = createdAt;
    }

    public Long getNotificationId() { return notificationId; }
    public Long getUserId() { return userId; }
    public NotificationType getType() { return type; }
    public String getContent() { return content; }
    public NotificationStatus getStatus() { return status; }
    public NotificationChannel getChannel() { return channel; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setNotificationId(Long notificationId) { this.notificationId = notificationId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setType(NotificationType type) { this.type = type; }
    public void setContent(String content) { this.content = content; }
    public void setStatus(NotificationStatus status) { this.status = status; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
