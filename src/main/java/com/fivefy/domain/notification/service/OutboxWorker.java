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
    private static final int MAX_RETRY_COUNT = 3;

    private final NotificationOutboxRepository outboxRepository;
    private final OutboxProcessor outboxProcessor;

    @Scheduled(fixedDelay = 10000)
    @SchedulerLock(name = "outboxWorker", lockAtMostFor = "PT30S", lockAtLeastFor = "PT1S")
    public void process() {
        List<NotificationOutbox> pending = outboxRepository
                .findAllByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING, PageRequest.of(0, BATCH_SIZE));

        if (pending.isEmpty()) return;

        log.info("[OutboxWorker] 처리 시작: {}건", pending.size());

        for (NotificationOutbox outbox : pending) {
            outboxProcessor.processOne(outbox.getId());
        }
    }

    @Scheduled(fixedDelay = 60000)
    @SchedulerLock(name = "outboxRetry", lockAtMostFor = "PT2M", lockAtLeastFor = "PT30S")
    public void retry() {
        List<NotificationOutbox> retryTargets = outboxRepository
                .findAllByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
                        OutboxStatus.FAILED, MAX_RETRY_COUNT, PageRequest.of(0, BATCH_SIZE));

        if (retryTargets.isEmpty()) return;

        log.info("[OutboxWorker] 재시도 시작: {}건", retryTargets.size());

        for (NotificationOutbox outbox : retryTargets) {
            outboxProcessor.retryOne(outbox.getId());
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    @SchedulerLock(name = "outboxCleanup", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    @Transactional
    public void cleanup() {
        LocalDateTime before = LocalDateTime.now().minusDays(CLEANUP_RETAIN_DAYS);
        int deleted = outboxRepository.deleteByStatusAndProcessedAtBefore(OutboxStatus.PROCESSED, before);
        log.info("[OutboxWorker] PROCESSED 정리 완료: {}건 삭제", deleted);
    }
}
