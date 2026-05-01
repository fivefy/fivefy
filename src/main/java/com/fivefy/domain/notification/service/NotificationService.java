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
import org.springframework.dao.DataIntegrityViolationException;
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
import java.time.Duration;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final long SSE_TIMEOUT = 60 * 60 * 1000L;
    private static final String SSE_EVENT_CONNECT = "connect";
    private static final String REDIS_NOTIFICATION_CHANNEL = "notification:";
    private static final String SSE_EVENT_NOTIFICATION = "notification";
    private static final int CHUNK_SIZE = 1000;
    private static final String IDEM_KEY_PREFIX = "notif:v1:";
    private static final Duration IDEM_TTL = Duration.ofDays(1);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SseEmitterRepository sseEmitterRepository;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final FollowRepository  followRepository;

    // 알림 구독
    public SseEmitter subscribe(Long userId, Long lastEventId) {
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

        // last-event-id 기반 미수신 알림 재전송
        if (lastEventId != null) {
            replayMissedNotifications(userId, lastEventId, emitter);
        }

        return emitter;
    }

    // 재연결 시 lastEventId 이후 알림 전송
    private void replayMissedNotifications(Long userId, Long lastEventId, SseEmitter emitter) {
        List<Notification> missed = notificationRepository.findMissedNotifications(
                userId, lastEventId, PageRequest.of(0, 100));

        if (missed.isEmpty()) {
            return;
        }

        log.info("미수신 알림 재전송: userId={}, lastEventId={}, 건수={}", userId, lastEventId, missed.size());

        for (Notification notification : missed) {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(notification.getId()))
                        .name(SSE_EVENT_NOTIFICATION)
                        .data(NotificationGetResponse.from(notification)));
            } catch (IOException e) {
                log.warn("미수신 알림 재전송 실패: userId={}, lastEventId={}", userId, lastEventId);
                sseEmitterRepository.delete(userId, emitter);
                break;
            }
        }
    }

    // 알림 발송 (수신 -> 저장 -> sse push)
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationEvent(NotificationEvent event) {
        send(event.targetUserId(), event.type(), event.content(),
                event.actorId(), event.resourceId());
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
    public void send(Long userId, NotificationType type, String content,
                     Long actorId, Long resourceId) {
        String idempotencyKey = buildIdempotencyKey(userId, type, actorId, resourceId);

        Notification saved;

        try {
            Boolean isFirst = stringRedisTemplate.opsForValue()
                    .setIfAbsent(IDEM_KEY_PREFIX + idempotencyKey, "1", IDEM_TTL);
            if (Boolean.FALSE.equals(isFirst)) {
                log.info("중복 알림 스킵 (Redis): userId={}, type={}, key={}", userId, type, idempotencyKey);
                return;
            }
        } catch (Exception e) {
            log.warn("Redis idempotency 실패, DB 제약으로 폴백: key={}", idempotencyKey);
        }

        try {
            Notification notification = Notification.create(
                    userId, content, type, NotificationChannel.IN_APP,
                    actorId, resourceId, idempotencyKey);
            saved = notificationRepository.save(notification);
        } catch (DataIntegrityViolationException e) {
            // 동일 키로 이미 발송된 알림 — 중복 스킵
            log.info("중복 알림 스킵: userId={}, type={}, key={}", userId, type, idempotencyKey);
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

            // redis 실패 보완 현재 연결된 emitter 직접 전송
            fallbackSseDirectPush(saved);
        }
    }

    /**
     * idempotency key 생성
     *
     * actorId / resourceId 가 있는 타입 → 행위자/리소스 기반으로 세분화
     *   NEW_FOLLOWER  : {userId}:NEW_FOLLOWER:{actorId}          (다른 사람이 팔로우 = 다른 알림)
     *   TRACK_LIKED   : {userId}:TRACK_LIKED:{resourceId}:{actorId}
     *   ALBUM_LIKED   : {userId}:ALBUM_LIKED:{resourceId}:{actorId}
     *   PUBLISH_TRACK : {userId}:PUBLISH_TRACK:{resourceId}      (같은 트랙 발매 알림 중복 방지)
     *
     *   SUBSCRIBE           : {userId}:SUBSCRIBE:{subscriptionId}
     *   SUBSCRIPTION_CANCEL : {userId}:SUBSCRIPTION_CANCEL:{subscriptionId}
     *   SUBSCRIPTION_EXPIRE : {userId}:SUBSCRIPTION_EXPIRE:{subscriptionId}
     */

    private String buildIdempotencyKey(Long userId, NotificationType type,
                                       Long actorId, Long resourceId) {
        return switch (type) {
            case NEW_FOLLOWER ->
                    userId + ":NEW_FOLLOWER:" + actorId;
            case TRACK_LIKED ->
                    userId + ":TRACK_LIKED:" + resourceId + ":" + actorId;
            case ALBUM_LIKED ->
                    userId + ":ALBUM_LIKED:" + resourceId + ":" + actorId;
            case PUBLISH_TRACK ->
                    userId + ":PUBLISH_TRACK:" + resourceId;
            case SUBSCRIBE ->
                    userId + ":SUBSCRIBE:" + resourceId; // resourceId = subscriptionId
            case SUBSCRIPTION_CANCEL ->
                    userId + ":SUBSCRIPTION_CANCEL:" + resourceId;
            case SUBSCRIPTION_EXPIRE ->
                    userId + ":SUBSCRIPTION_EXPIRE:" + resourceId;
        };
    }

    private void fallbackSseDirectPush(Notification notification) {
        List<SseEmitter> emitters = sseEmitterRepository.findAllByUserId(notification.getUserId());
        if (emitters.isEmpty()) {
            log.warn("SSE 직접 전송 불가 (연결된 emitter 없음): userId{}", notification.getUserId());
            return;
        }

        for (SseEmitter emitter : emitters) {
            try{
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(notification.getId()))
                        .name(SSE_EVENT_NOTIFICATION)
                        .data(NotificationGetResponse.from(notification)));
                log.info("SSE 직접 전송 성공: userId={}, notificationId={}",
                        notification.getUserId(), notification.getId());
            } catch (IOException e) {
                log.warn("SSE 직접 전송 실패: userID={}", notification.getUserId());
                sseEmitterRepository.delete(notification.getUserId(), emitter);
            }
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
