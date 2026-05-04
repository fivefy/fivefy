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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxWorkerTest {

    @Mock private NotificationOutboxRepository outboxRepository;
    @Mock private NotificationService notificationService;

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
        @DisplayName("PENDING 처리 성공 시 PROCESSED 상태로 변경된다")
        void process_success_marksAsProcessed() {
            // given
            NotificationOutbox outbox = makeOutbox();
            given(outboxRepository.findAllByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(Pageable.class)))
                    .willReturn(List.of(outbox));

            // when
            outboxWorker.process();

            // then
            assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
            verify(outboxRepository).save(outbox);
        }

        @Test
        @DisplayName("PENDING 처리 실패 시 FAILED 상태로 변경된다")
        void process_fails_marksAsFailed() {
            // given
            NotificationOutbox outbox = makeOutbox();
            given(outboxRepository.findAllByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(Pageable.class)))
                    .willReturn(List.of(outbox));
            willThrow(new RuntimeException("Redis 장애"))
                    .given(notificationService).send(any(), any(), any(), any(), any());

            // when
            outboxWorker.process();

            // then
            assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.FAILED);
            verify(outboxRepository).save(outbox);
        }
    }

    @Nested
    @DisplayName("retry()")
    class Retry {

        @Test
        @DisplayName("FAILED 재시도 성공 시 PROCESSED 상태로 변경된다")
        void retry_success_marksAsProcessed() {
            // given
            NotificationOutbox outbox = makeOutbox();
            given(outboxRepository.findAllByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
                    eq(OutboxStatus.FAILED), eq(3), any(Pageable.class)))
                    .willReturn(List.of(outbox));

            // when
            outboxWorker.retry();

            // then
            assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
            assertThat(outbox.getRetryCount()).isEqualTo(1);
            verify(outboxRepository).save(outbox);
        }

        @Test
        @DisplayName("FAILED 재시도 실패 시 retryCount가 증가한다")
        void retry_fails_incrementsRetryCount() {
            // given
            NotificationOutbox outbox = makeOutbox();
            outbox.markAsFailed();
            given(outboxRepository.findAllByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
                    eq(OutboxStatus.FAILED), eq(3), any(Pageable.class)))
                    .willReturn(List.of(outbox));
            willThrow(new RuntimeException("Redis 장애"))
                    .given(notificationService).send(any(), any(), any(), any(), any());

            // when
            outboxWorker.retry();

            // then
            assertThat(outbox.getRetryCount()).isEqualTo(1);
            assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.FAILED);
            verify(outboxRepository).save(outbox);
        }

        @Test
        @DisplayName("retryCount가 3이면 조회 대상에서 제외되어 재시도하지 않는다")
        void retry_maxRetryCount_skipsProcessing() {
            // given
            given(outboxRepository.findAllByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
                    eq(OutboxStatus.FAILED), eq(3), any(Pageable.class)))
                    .willReturn(List.of());

            // when
            outboxWorker.retry();

            // then — notificationService.send() 호출 없음
            verify(notificationService, org.mockito.Mockito.never())
                    .send(any(), any(), any(), any(), any());
        }
    }
}
