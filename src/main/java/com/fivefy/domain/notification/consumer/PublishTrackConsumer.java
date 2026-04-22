package com.fivefy.domain.notification.consumer;

import com.fivefy.common.config.rabbitmq.NotificationRabbitConfig;
import com.fivefy.domain.follow.entity.Follow;
import com.fivefy.domain.follow.repository.FollowRepository;
import com.fivefy.domain.notification.enums.NotificationType;
import com.fivefy.domain.notification.service.NotificationService;
import com.fivefy.domain.track.event.PublishTrackEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublishTrackConsumer {

    private static final int CHUNK_SIZE = 1000;

    private final FollowRepository followRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = NotificationRabbitConfig.PUBLISH_TRACK_QUEUE)
    public void consume(String message) {
        try {
            PublishTrackEvent event = objectMapper.readValue(message, PublishTrackEvent.class);

            log.info("RabbitMQ PUBLISH_TRACK 수신: artistId={}, trackId={}",
                    event.artistId(), event.trackId());

            String content = "새 트랙 \"" + event.trackTitle() + "\"이 발매되었습니다";

            int page = 0;
            Page<Follow> chunk;

            do {
                chunk = followRepository.findAllByArtistIdAndNotificationEnabledTrue(
                        event.artistId(), PageRequest.of(page++, CHUNK_SIZE));

                chunk.getContent().forEach(follow ->
                        notificationService.send(
                                follow.getUserId(),
                                NotificationType.PUBLISH_TRACK,
                                content));

                log.debug("PUBLISH_TRACK 청크 처리 완료: page={}, size={}, artistId={}",
                        page - 1, chunk.getNumberOfElements(), event.artistId());

            } while (chunk.hasNext());

            log.info("PUBLISH_TRACK 알림 발송 완료: artistId={}", event.artistId());

        } catch (Exception e) {
            log.error("RabbitMQ 메시지 처리 실패: {}", e.getMessage());
        }
    }
}