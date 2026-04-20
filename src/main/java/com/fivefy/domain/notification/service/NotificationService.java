package com.fivefy.domain.notification.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.notification.dto.response.NotificationGetResponse;
import com.fivefy.domain.notification.entity.Notification;
import com.fivefy.domain.notification.enums.NotificationChannel;
import com.fivefy.domain.notification.enums.NotificationType;
import com.fivefy.domain.notification.event.NotificationEvent;
import com.fivefy.domain.notification.repository.NotificationRepository;
import com.fivefy.domain.notification.repository.SseEmitterRepository;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

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

    // 알림 구독
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        sseEmitterRepository.save(userId, emitter);

        emitter.onCompletion(() -> sseEmitterRepository.deleteByUserId(userId));
        emitter.onTimeout(() -> {
            sseEmitterRepository.deleteByUserId(userId);
            emitter.complete();
        });
        emitter.onError((e) -> sseEmitterRepository.deleteByUserId(userId));


        // 연결 직후 초기 이벤트 전송
        try {
            Long unreadCount = notificationRepository.countByUserIdAndReadAtIsNull(userId);
            emitter.send(SseEmitter.event()
                    .name(SSE_EVENT_CONNECT)
                    .data(unreadCount));
        } catch (IOException e) {
            log.warn("SSE 초기 이벤트 전송 실패: userId={}", userId);
            sseEmitterRepository.deleteByUserId(userId);
        }

        return emitter;
    }

    @Async
    @EventListener
    public void handleBotificationEvent(NotificationEvent event) {
        send(event.targetUserId(), event.type(), event.content());
    }

    @Transactional
    public void send(Long userId, NotificationType type, String content) {
        Notification notification = Notification.create(userId, content, type, NotificationChannel.IN_APP);
        notificationRepository.save(notification);

        sseEmitterRepository.findByUserId(userId).ifPresent(sseEmitter -> {
            try {
                sseEmitter.send(SseEmitter.event()
                        .name(SSE_EVENT_NOTIFICATION)
                        .data(NotificationGetResponse.from(notification)));
                notification.markAsSent();
            } catch (IOException e) {
                log.warn("SSE 전송 실패: userId={}, type={}", userId, type);
                notification.markAsFailed();
                sseEmitterRepository.deleteByUserId(userId);
            }
        });
    }

    @Transactional(readOnly = true)
    public Page<NotificationGetResponse> getNotifications(Long userId, Pageable pageable) {
        User user = getUser(userId);

        return notificationRepository.findAllByUserId(user.getId(), pageable)
                .map(NotificationGetResponse::from);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.ERR_USER_NOT_FOUND));
    }
}
