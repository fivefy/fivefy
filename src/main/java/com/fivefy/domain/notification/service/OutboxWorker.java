package com.fivefy.domain.notification.service;

import com.fivefy.domain.notification.entity.NotificationOutbox;
import com.fivefy.domain.notification.enums.OutboxStatus;
import com.fivefy.domain.notification.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxWorker {

    private static final int BATCH_SIZE = 100;
    private static final int CLEANUP_RETAIN_DAYS = 7;

    private final NotificationOutboxRepository outboxRepository;
    private final NotificationService notificationService;

    @Scheduled(fixedDelay = 1000)
    @SchedulerLock(name = "outboxWorker", lockAtMostFor = "PT30S", lockAtLeastFor = "PT1S")
    @Transactional
    public void process() {
        List<NotificationOutbox> pending = outboxRepository
                .findAllByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING, PageRequest.of(0, BATCH_SIZE));

        if (pending.isEmpty()) return;

        log.info("[OutboxWorker] 처리 시작: {}건", pending.size());

        for (NotificationOutbox outbox : pending) {
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
                log.error("[OutboxWorker] 처리 실패: outboxId={}, type={}, 사유={}",
                        outbox.getId(), outbox.getEventType(), e.getMessage());
                outbox.markAsFailed();
            }
            outboxRepository.save(outbox);
        }
    }

    @Scheduled(cron = "0 0 0 * * *") // 매일 오전 00:00
    @SchedulerLock(name = "outboxCleanup", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    @Transactional
    public void cleanup() {
        LocalDateTime before = LocalDateTime.now().minusDays(CLEANUP_RETAIN_DAYS);
        int deleted = outboxRepository.deleteByStatusAndProcessedAtBefore(OutboxStatus.PROCESSED, before);
        log.info("[OutboxWorker] PROCESSED 정리 완료: {}건 삭제", deleted);
    }
}
