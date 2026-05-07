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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

    @Mock private NotificationService notificationService;
    @Mock private NotificationOutboxRepository outboxRepository;

    @InjectMocks
    private OutboxProcessor outboxProcessor;

    private NotificationOutbox makeOutbox() {
        return NotificationOutbox.create(
                NotificationType.NEW_FOLLOWER, 1L, 2L, null, "테스트유저님이 팔로우했습니다");
    }

    @Nested
    @DisplayName("processOne()")
    class ProcessOne {

        @Test
        @DisplayName("처리 성공 시 PROCESSED 상태로 변경된다")
        void processOne_success_marksAsProcessed() {
            // given
            NotificationOutbox outbox = makeOutbox();
            given(outboxRepository.findById(outbox.getId())).willReturn(Optional.of(outbox));

            // when
            outboxProcessor.processOne(outbox.getId());

            // then
            assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
            verify(outboxRepository).save(outbox);
        }

        @Test
        @DisplayName("처리 실패 시 FAILED 상태로 변경된다")
        void processOne_fails_marksAsFailed() {
            // given
            NotificationOutbox outbox = makeOutbox();
            given(outboxRepository.findById(outbox.getId())).willReturn(Optional.of(outbox));
            willThrow(new RuntimeException("Redis 장애"))
                    .given(notificationService).send(any(), any(), any(), any(), any());

            // when
            outboxProcessor.processOne(outbox.getId());

            // then
            assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.FAILED);
            verify(outboxRepository).save(outbox);
        }
    }

    @Nested
    @DisplayName("retryOne()")
    class RetryOne {

        @Test
        @DisplayName("재시도 성공 시 PROCESSED 상태로 변경되고 retryCount가 증가한다")
        void retryOne_success_marksAsProcessed() {
            // given
            NotificationOutbox outbox = makeOutbox();
            given(outboxRepository.findById(outbox.getId())).willReturn(Optional.of(outbox));

            // when
            outboxProcessor.retryOne(outbox.getId());

            // then
            assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
            assertThat(outbox.getRetryCount()).isEqualTo(1);
            verify(outboxRepository).save(outbox);
        }

        @Test
        @DisplayName("재시도 실패 시 retryCount가 증가하고 FAILED 상태를 유지한다")
        void retryOne_fails_incrementsRetryCount() {
            // given
            NotificationOutbox outbox = makeOutbox();
            outbox.markAsFailed();
            given(outboxRepository.findById(outbox.getId())).willReturn(Optional.of(outbox));
            willThrow(new RuntimeException("Redis 장애"))
                    .given(notificationService).send(any(), any(), any(), any(), any());

            // when
            outboxProcessor.retryOne(outbox.getId());

            // then
            assertThat(outbox.getRetryCount()).isEqualTo(1);
            assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.FAILED);
            verify(outboxRepository).save(outbox);
        }
    }
}
