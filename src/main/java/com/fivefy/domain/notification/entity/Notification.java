package com.fivefy.domain.notification.entity;

import com.fivefy.common.entity.BaseEntity;
import com.fivefy.domain.notification.enums.NotificationChannel;
import com.fivefy.domain.notification.enums.NotificationStatus;
import com.fivefy.domain.notification.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static com.fivefy.common.util.ValidationUtils.validateNonNull;

@Getter
@Entity
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    private LocalDateTime readAt;

    private Long actorId;    // 행위자 ID (팔로우한 유저, 좋아요 누른 유저 등)

    private Long resourceId; // 대상 리소스 ID (트랙 ID, 앨범 ID 등)

    @Column(unique = true)
    private String idempotencyKey;

    public static Notification create(Long userId, String content, NotificationType type,
                                      NotificationChannel channel,
                                      Long actorId, Long resourceId,
                                      String idempotencyKey) {
        validateNonNull(userId, "userId");
        validateNonNull(content, "content");
        validateNonNull(type, "type");
        validateNonNull(channel, "channel");

        Notification notification = new Notification();
        notification.userId = userId;
        notification.type = type;
        notification.content = content;
        notification.status = NotificationStatus.QUEUED;
        notification.channel = channel;
        notification.actorId = actorId;
        notification.resourceId = resourceId;
        notification.idempotencyKey = idempotencyKey;

        return notification;
    }

    public void markAsSent() {
        this.status = NotificationStatus.SENT;
    }

    public void markAsFailed() {
        this.status = NotificationStatus.FAILED;
    }

    public void markAsRead() {
        this.readAt = LocalDateTime.now();
    }

    public boolean isRead() {
        return this.readAt != null;
    }
}
