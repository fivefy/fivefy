package com.fivefy.domain.notification.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.follow.entity.Follow;
import com.fivefy.domain.follow.repository.FollowRepository;
import com.fivefy.domain.notification.dto.response.NotificationGetResponse;
import com.fivefy.domain.notification.entity.Notification;
import com.fivefy.domain.notification.enums.NotificationChannel;
import com.fivefy.domain.notification.enums.NotificationErrorCode;
import com.fivefy.domain.notification.enums.NotificationType;
import com.fivefy.domain.notification.event.NotificationEvent;
import com.fivefy.domain.notification.repository.NotificationRepository;
import com.fivefy.domain.notification.repository.SseEmitterRepository;
import com.fivefy.domain.track.event.PublishTrackEvent;
import com.fivefy.domain.user.entity.User;
import com.fivefy.domain.user.enums.UserErrorCode;
import com.fivefy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final long SSE_TIMEOUT = 60 * 60 * 1000L;
    private static final String SSE_EVENT_CONNECT = "connect";
    private static final String SSE_EVENT_NOTIFICATION = "notification";

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SseEmitterRepository sseEmitterRepository;
    private final FollowRepository followRepository;

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

    // 트랙 발행 → 알림 수신 동의 팔로워 전체 알림
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePublishTrackEvent(PublishTrackEvent event) {
        List<Long> targetUserIds = followRepository
                .findAllByArtistIdAndNotificationEnabledTrue(event.artistId())
                .stream()
                .map(Follow::getUserId)
                .toList();

        log.info("PUBLISH_TRACK 알림 발송: artistId={}, trackId={}, 대상 팔로워 수={}",
                event.artistId(), event.trackId(), targetUserIds.size());

        String content = "새 트랙 \"" + event.trackTitle() + "\"이 발매되었습니다";

        targetUserIds.forEach(userId ->
                send(userId, NotificationType.PUBLISH_TRACK, content));
    }

    public void send(Long userId, NotificationType type, String content) {

        // DB 저장 — save() 자체 트랜잭션으로 처리 후 커넥션 즉시 반환
        Notification notification = Notification.create(userId, content, type, NotificationChannel.IN_APP);
        Notification saved = notificationRepository.save(notification);

        // SSE 전송 — DB 커넥션 미점유 상태
        List<SseEmitter> emitterList = sseEmitterRepository.findAllByUserId(userId);
        int originalEmitterCount = emitterList.size();
        boolean sent = false;

        for (SseEmitter emitter : emitterList) {
            try {
                saved.markAsSent();
                emitter.send(SseEmitter.event()
                        .name(SSE_EVENT_NOTIFICATION)
                        .data(NotificationGetResponse.from(saved)));
                sent = true;
            } catch (IOException e) {
                log.warn("SSE 전송 실패: userId={}, type={}", userId, type);
                saved.markAsFailed();
                sseEmitterRepository.delete(userId, emitter);
            }
        }

        if (sent || originalEmitterCount > 0) {
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
