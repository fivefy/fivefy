package com.fivefy.domain.notification.service;

import tools.jackson.databind.ObjectMapper;
import com.fivefy.domain.notification.dto.NotificationMessage;
import com.fivefy.domain.notification.dto.response.NotificationGetResponse;
import com.fivefy.domain.notification.repository.SseEmitterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRedisSubscriber implements MessageListener {

    private static final String SSE_EVENT_NOTIFICATION = "notification";

    private final SseEmitterRepository sseEmitterRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            NotificationMessage payload = objectMapper.readValue(json, NotificationMessage.class);

            Long userId = payload.getUserId();
            List<SseEmitter> emitterList = sseEmitterRepository.findAllByUserId(userId);

            for (SseEmitter emitter : emitterList) {
                try {
                    emitter.send(SseEmitter.event()
                            .name(SSE_EVENT_NOTIFICATION)
                            .data(toResponse(payload)));
                    log.debug("Redis Pub/Sub SSE 전송 성공: userId={}, type={}", userId, payload.getType());
                } catch (IOException e) {
                    log.warn("Redis Pub/Sub SSE 전송 실패: userId={}", userId);
                    sseEmitterRepository.delete(userId, emitter);
                }
            }
        } catch (Exception e) {
            log.error("Redis 메시지 처리 실패: {}", e.getMessage());
        }
    }

    private NotificationGetResponse toResponse(NotificationMessage msg) {
        return new NotificationGetResponse(
                msg.getNotificationId(),
                msg.getType(),
                msg.getContent(),
                msg.getStatus(),
                msg.getChannel(),
                null,
                msg.getCreatedAt()
        );
    }
}