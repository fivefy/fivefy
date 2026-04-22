package com.fivefy.domain.notification.consumer;

import com.fivefy.common.config.rabbitmq.NotificationRabbitConfig;
import com.fivefy.domain.notification.enums.NotificationType;
import com.fivefy.domain.notification.service.NotificationService;
import com.fivefy.domain.track.event.PublishTrackChunkEvent;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
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

    @RabbitListener(
            queues = NotificationRabbitConfig.PUBLISH_TRACK_QUEUE,
            concurrency = "3-10",
            ackMode = "MANUAL"
    )
    public void consume(String message,
                        Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                        @Header(value = "x-death", required = false) List<Map<String, Object>> xDeath) {
        try {
            PublishTrackChunkEvent event = objectMapper.readValue(message, PublishTrackChunkEvent.class);

            // 재시도 횟수 확인 — MAX_RETRY_COUNT 초과 시 DLQ로 이동
            long retryCount = getRetryCount(xDeath);
            if (retryCount >= NotificationRabbitConfig.MAX_RETRY_COUNT) {
                log.error("최대 재시도 초과, DLQ로 이동: trackId={}, chunkIndex={}, retryCount={}",
                        event.trackId(), event.chunkIndex(), retryCount);
                channel.basicNack(deliveryTag, false, false); // requeue=false → DLQ로 이동
                return;
            }

            // 중복 청크 체크
            String dedupKey = DEDUP_KEY_PREFIX + event.trackId() + ":chunk:" + event.chunkIndex();
            Boolean isNew = stringRedisTemplate.opsForValue()
                    .setIfAbsent(dedupKey, "1", DEDUP_TTL_HOURS, TimeUnit.HOURS);

            if (Boolean.FALSE.equals(isNew)) {
                log.warn("중복 청크 감지, 처리 스킵: trackId={}, chunkIndex={}",
                        event.trackId(), event.chunkIndex());
                channel.basicAck(deliveryTag, false);
                return;
            }

            log.debug("PUBLISH_TRACK 청크 처리 시작: trackId={}, artistId={}, 청크 size={}, 재시도={}",
                    event.trackId(), event.artistId(), event.userIds().size(), retryCount);

            String content = "새 트랙 \"" + event.trackTitle() + "\"이 발매되었습니다";

            event.userIds().forEach(userId ->
                    notificationService.send(userId, NotificationType.PUBLISH_TRACK, content));

            log.debug("PUBLISH_TRACK 청크 처리 완료: trackId={}, 청크 size={}",
                    event.trackId(), event.userIds().size());

            channel.basicAck(deliveryTag, false); // 성공 → ACK

        } catch (Exception e) {
            log.error("RabbitMQ 청크 메시지 처리 실패: {}", e.getMessage());
            try {
                channel.basicNack(deliveryTag, false, true); // 실패 → 재큐
            } catch (Exception nackEx) {
                log.error("NACK 실패: {}", nackEx.getMessage());
            }
        }
    }

    @RabbitListener(
            queues = NotificationRabbitConfig.PUBLISH_TRACK_DLQ,
            ackMode = "MANUAL"
    )
    public void consumeDlq(String message,
                           Channel channel,
                           @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            PublishTrackChunkEvent event = objectMapper.readValue(message, PublishTrackChunkEvent.class);
            log.error("DLQ 메시지 수신 — 수동 처리 필요: trackId={}, artistId={}, chunkIndex={}",
                    event.trackId(), event.artistId(), event.chunkIndex());

            // DLQ는 ACK만 처리 — 재처리 로직은 수동 개입 필요
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("DLQ 메시지 파싱 실패: {}", e.getMessage());
            try {
                channel.basicAck(deliveryTag, false); // 파싱 실패도 ACK — 무한 루프 방지
            } catch (Exception ackEx) {
                log.error("DLQ ACK 실패: {}", ackEx.getMessage());
            }
        }
    }

    private long getRetryCount(List<Map<String, Object>> xDeath) {
        if (xDeath == null || xDeath.isEmpty()) return 0;
        Object count = xDeath.get(0).get("count");
        return count instanceof Long ? (Long) count : 0L;
    }
}
