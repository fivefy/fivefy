package com.fivefy.domain.notification.consumer;

import com.fivefy.common.config.rabbitmq.NotificationRabbitConfig;
import com.fivefy.domain.notification.enums.NotificationType;
import com.fivefy.domain.notification.service.NotificationService;
import com.fivefy.domain.track.event.PublishTrackChunkEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublishTrackConsumer {

    private static final String DEDUP_KEY_PREFIX = "notification:dedup:publish:";
    private static final long DEDUP_TTL_HOURS = 24;

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;

    // concurrency: 최소 3개 ~ 최대 10개 Consumer 스레드가 병렬 처리
    @RabbitListener(
            queues = NotificationRabbitConfig.PUBLISH_TRACK_QUEUE,
            concurrency = "3-10"
    )
    public void consume(String message) {
        try {
            PublishTrackChunkEvent event = objectMapper.readValue(message, PublishTrackChunkEvent.class);

            // 청크 단위 중복 체크 — trackId + 청크 첫 번째 userId 조합
            String dedupKey = DEDUP_KEY_PREFIX + event.trackId() + ":chunk:" + event.chunkIndex();
            Boolean isNew = stringRedisTemplate.opsForValue()
                    .setIfAbsent(dedupKey, "1", DEDUP_TTL_HOURS, TimeUnit.HOURS);

            if (Boolean.FALSE.equals(isNew)) {
                log.warn("중복 청크 감지, 처리 스킵: trackId={}, 청크 size={}",
                        event.trackId(), event.userIds().size());
                return;
            }

            log.debug("PUBLISH_TRACK 청크 처리 시작: trackId={}, artistId={}, 청크 size={}",
                    event.trackId(), event.artistId(), event.userIds().size());

            String content = "새 트랙 \"" + event.trackTitle() + "\"이 발매되었습니다";

            event.userIds().forEach(userId ->
                    notificationService.send(userId, NotificationType.PUBLISH_TRACK, content));

            log.debug("PUBLISH_TRACK 청크 처리 완료: trackId={}, 청크 size={}",
                    event.trackId(), event.userIds().size());

        } catch (Exception e) {
            log.error("RabbitMQ 청크 메시지 처리 실패: {}", e.getMessage());
        }
    }
}
