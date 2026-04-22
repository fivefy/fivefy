package com.fivefy.domain.notification.service;

import com.fivefy.common.config.rabbitmq.NotificationRabbitConfig;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.follow.entity.Follow;
import com.fivefy.domain.follow.repository.FollowRepository;
import com.fivefy.domain.notification.dto.NotificationMessage;
import com.fivefy.domain.notification.dto.response.NotificationGetResponse;
import com.fivefy.domain.notification.entity.Notification;
import com.fivefy.domain.notification.enums.NotificationChannel;
import com.fivefy.domain.notification.enums.NotificationErrorCode;
import com.fivefy.domain.notification.enums.NotificationStatus;
import com.fivefy.domain.notification.enums.NotificationType;
import com.fivefy.domain.notification.event.NotificationEvent;
import com.fivefy.domain.notification.repository.NotificationRepository;
import com.fivefy.domain.notification.repository.SseEmitterRepository;
import com.fivefy.domain.track.event.PublishTrackChunkEvent;
import com.fivefy.domain.track.event.PublishTrackEvent;
import com.fivefy.domain.user.entity.User;
import com.fivefy.domain.user.enums.UserErrorCode;
import com.fivefy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final long SSE_TIMEOUT = 60 * 60 * 1000L;
    private static final String SSE_EVENT_CONNECT = "connect";
    private static final String REDIS_NOTIFICATION_CHANNEL = "notification:";
    private static final int CHUNK_SIZE = 1000;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SseEmitterRepository sseEmitterRepository;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final FollowRepository  followRepository;

    // 알림 구독
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        sseEmitterRepository.save(userId, emitter);

        emitter.onCompletion(() -> sseEmitterRepository.delete(userId, emitter));
        emitter.onTimeout(() -> {
            sseEmitterRepository.delete(userId, emitter);
            emitter.complete();
        });
        emitter.onError(e -> sseEmitterRepository.delete(userId, emitter));


        // 연결 직후 초기 이벤트 전송
        try {
            Long unreadCount = notificationRepository.countByUserIdAndReadAtIsNull(userId);
            emitter.send(SseEmitter.event()
                    .name(SSE_EVENT_CONNECT)
                    .data(unreadCount));
        } catch (IOException e) {
            log.warn("SSE 초기 이벤트 전송 실패: userId={}", userId);
            sseEmitterRepository.delete(userId, emitter);
        }

        return emitter;
    }

    // 알림 발송 (수신 -> 저장 -> sse push)
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationEvent(NotificationEvent event) {
        send(event.targetUserId(), event.type(), event.content());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePublishTrackEvent(PublishTrackEvent event) {
        try {
            int page = 0;
            int totalChunks = 0;
            long chunkIndex = 0L;
            Page<Follow> chunk;

            do {
                chunk = followRepository.findAllByArtistIdAndNotificationEnabledTrue(
                        event.artistId(), PageRequest.of(page++, CHUNK_SIZE));

                if (chunk.isEmpty()) break;

                List<Long> userIds = chunk.getContent().stream()
                        .map(Follow::getUserId)
                        .toList();

                // 청크별 메시지 발행 → 각 Consumer가 병렬 처리
                String message = objectMapper.writeValueAsString(
                        PublishTrackChunkEvent.of(
                                event.artistId(),
                                event.trackId(),
                                event.trackTitle(),
                                chunkIndex++,
                                userIds));

                rabbitTemplate.convertAndSend(
                        NotificationRabbitConfig.NOTIFICATION_EXCHANGE,
                        NotificationRabbitConfig.PUBLISH_TRACK_ROUTING_KEY,
                        message);

                totalChunks++;

            } while (chunk.hasNext());

            log.info("PUBLISH_TRACK RabbitMQ 발행 완료: artistId={}, trackId={}, 총 청크 수={}",
                    event.artistId(), event.trackId(), totalChunks);

        } catch (Exception e) {
            log.error("PUBLISH_TRACK RabbitMQ 발행 실패: artistId={}", event.artistId());
        }
    }

    // Redis publish
    public void send(Long userId, NotificationType type, String content) {
        Notification notification = Notification.create(userId, content, type, NotificationChannel.IN_APP);
        Notification saved = notificationRepository.save(notification);

        boolean hasConnection = !sseEmitterRepository.findAllByUserId(userId).isEmpty();
        if (!hasConnection) {
            return;
        }

        try {
            NotificationMessage message = new NotificationMessage(
                    saved.getId(),
                    userId,
                    type,
                    content,
                    NotificationStatus.SENT,
                    saved.getChannel(),
                    saved.getCreatedAt()
            );
            String channel = REDIS_NOTIFICATION_CHANNEL + userId;
            stringRedisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(message)); // 전송

            saved.markAsSent();
            notificationRepository.save(saved);
        } catch (Exception e) {
            log.error("알림 발송 실패: userId={}, type={}", userId, type);
            saved.markAsFailed();
            notificationRepository.save(saved);
        }
    }

    // 알림 목록 조회
    @Transactional(readOnly = true)
    public Page<NotificationGetResponse> getNotifications(Long userId, Pageable pageable) {
        User user = getUser(userId);

        return notificationRepository.findAllByUserId(user.getId(), pageable)
                .map(NotificationGetResponse::from);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    // 읽음 처리
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(NotificationErrorCode.ERR_NOTIFICATION_NOT_FOUND));

        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException(NotificationErrorCode.ERR_NOTIFICATION_UNAUTHORIZED);
        }

        notification.markAsRead();
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Transactional
    public void deleteNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(NotificationErrorCode.ERR_NOTIFICATION_NOT_FOUND));

        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException(NotificationErrorCode.ERR_NOTIFICATION_UNAUTHORIZED);
        }

        notificationRepository.delete(notification);
    }

    @Transactional
    public void deleteAllNotifications(Long userId) {
        notificationRepository.deleteAllByUserId(userId);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.ERR_USER_NOT_FOUND));
    }
}
