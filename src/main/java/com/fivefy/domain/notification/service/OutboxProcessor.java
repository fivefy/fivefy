package com.fivefy.domain.notification.service;

import com.fivefy.domain.notification.entity.NotificationOutbox;
import com.fivefy.domain.notification.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxProcessor {

    private final NotificationService notificationService;
    private final NotificationOutboxRepository outboxRepository;

    /**
     * 단일 Outbox 항목을 독립 트랜잭션으로 처리
     * REQUIRES_NEW: 외부 트랜잭션과 분리되어 개별 커밋/롤백
     * → 다른 항목 처리 실패로 인한 롤백이 이 항목에 영향을 주지 않음
     * → Redis key 설정 후 트랜잭션 롤백으로 인한 알림 유실 방지
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOne(Long outboxId) {
        NotificationOutbox outbox = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new IllegalStateException("outbox not found: " + outboxId));
        try {
            notificationService.send(
                    outbox.getTargetUserId(),
                    outbox.getEventType(),
                    outbox.getContent(),
                    outbox.getActorId(),
                    outbox.getResourceId()
            );
            outbox.markAsProcessed();
        } catch (Exception e) {
            log.error("[OutboxProcessor] 처리 실패: outboxId={}, type={}, 사유={}",
                    outbox.getId(), outbox.getEventType(), e.getMessage());
            outbox.markAsFailed();
        }
        outboxRepository.save(outbox);
    }

    /**
     * 단일 Outbox 항목을 독립 트랜잭션으로 재시도
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void retryOne(Long outboxId) {
        NotificationOutbox outbox = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new IllegalStateException("outbox not found: " + outboxId));
        outbox.incrementRetry();
        try {
            notificationService.send(
                    outbox.getTargetUserId(),
                    outbox.getEventType(),
                    outbox.getContent(),
                    outbox.getActorId(),
                    outbox.getResourceId()
            );
            outbox.markAsProcessed();
            log.info("[OutboxProcessor] 재시도 성공: outboxId={}, retryCount={}",
                    outbox.getId(), outbox.getRetryCount());
        } catch (Exception e) {
            log.error("[OutboxProcessor] 재시도 실패: outboxId={}, retryCount={}, 사유={}",
                    outbox.getId(), outbox.getRetryCount(), e.getMessage());
        }
        outboxRepository.save(outbox);
    }
}