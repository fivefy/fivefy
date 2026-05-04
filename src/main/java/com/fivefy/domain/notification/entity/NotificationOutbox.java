package com.fivefy.domain.notification.entity;

import com.fivefy.domain.notification.enums.NotificationType;
import com.fivefy.domain.notification.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static com.fivefy.common.util.ValidationUtils.validateNonNull;

@Getter
@Entity
@Table(name = "notification_outbox")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType eventType;

    @Column(nullable = false)
    private Long targetUserId;

    private Long actorId;

    private Long resourceId;

    @Column(nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    public static NotificationOutbox create(NotificationType eventType, Long targetUserId,
                                            Long actorId, Long resourceId, String content) {
        validateNonNull(eventType, "eventType");
        validateNonNull(targetUserId, "targetUserId");
        validateNonNull(content, "content");

        NotificationOutbox outbox = new NotificationOutbox();
        outbox.eventType = eventType;
        outbox.targetUserId = targetUserId;
        outbox.actorId = actorId;
        outbox.resourceId = resourceId;
        outbox.content = content;
        outbox.status = OutboxStatus.PENDING;
        outbox.retryCount = 0;
        outbox.createdAt = LocalDateTime.now();
        return outbox;
    }

    public void markAsProcessed() {
        this.status = OutboxStatus.PROCESSED;
        this.processedAt = LocalDateTime.now();
    }

    public void incrementRetry() {
        this.retryCount++;
    }

    public void markAsFailed() {
        this.status = OutboxStatus.FAILED;
    }
}
