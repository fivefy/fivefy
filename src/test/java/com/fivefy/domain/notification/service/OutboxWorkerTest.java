package com.fivefy.domain.notification.service;

import com.fivefy.domain.notification.entity.NotificationOutbox;
import com.fivefy.domain.notification.enums.NotificationType;
import com.fivefy.domain.notification.enums.OutboxStatus;
import com.fivefy.domain.notification.repository.NotificationOutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxWorkerTest {

    @Mock private NotificationOutboxRepository outboxRepository;
    @Mock private OutboxProcessor outboxProcessor;

    @InjectMocks
    private OutboxWorker outboxWorker;

    private NotificationOutbox makeOutbox() {
        return NotificationOutbox.create(
                NotificationType.NEW_FOLLOWER, 1L, 2L, null, "테스트유저님이 팔로우했습니다");
    }

    @Nested
    @DisplayName("process()")
    class Process {

        @Test
        @DisplayName("PENDING 항목이 있으면 outboxProcessor.processOne()을 호출한다")
        void process_delegatesToProcessor() {
            // given
            NotificationOutbox outbox = makeOutbox();
            given(outboxRepository.findAllByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(Pageable.class)))
                    .willReturn(List.of(outbox));

            // when
            outboxWorker.process();

            // then
            verify(outboxProcessor).processOne(outbox.getId());
        }

        @Test
        @DisplayName("PENDING 항목이 없으면 outboxProcessor를 호출하지 않는다")
        void process_noPending_doesNotDelegate() {
            // given
            given(outboxRepository.findAllByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(Pageable.class)))
                    .willReturn(List.of());

            // when
            outboxWorker.process();

            // then
            verify(outboxProcessor, org.mockito.Mockito.never()).processOne(any());
        }
    }

    @Nested
    @DisplayName("retry()")
    class Retry {

        @Test
        @DisplayName("FAILED 항목이 있으면 outboxProcessor.retryOne()을 호출한다")
        void retry_delegatesToProcessor() {
            // given
            NotificationOutbox outbox = makeOutbox();
            given(outboxRepository.findAllByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
                    eq(OutboxStatus.FAILED), eq(3), any(Pageable.class)))
                    .willReturn(List.of(outbox));

            // when
            outboxWorker.retry();

            // then
            verify(outboxProcessor).retryOne(outbox.getId());
        }

        @Test
        @DisplayName("retryCount가 3이면 조회 대상에서 제외되어 retryOne()을 호출하지 않는다")
        void retry_maxRetryCount_skipsProcessing() {
            // given
            given(outboxRepository.findAllByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
                    eq(OutboxStatus.FAILED), eq(3), any(Pageable.class)))
                    .willReturn(List.of());

            // when
            outboxWorker.retry();

            // then
            verify(outboxProcessor, org.mockito.Mockito.never()).retryOne(any());
        }
    }
}
