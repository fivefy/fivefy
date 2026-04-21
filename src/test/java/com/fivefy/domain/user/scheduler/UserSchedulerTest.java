package com.fivefy.domain.user.scheduler;

import com.fivefy.domain.user.enums.UserStatus;
import com.fivefy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSchedulerTest {

    @InjectMocks
    private UserScheduler userScheduler;

    @Mock private UserRepository userRepository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @Nested
    @DisplayName("익명화 스케줄러")
    class AnonymizeDeletedUsers {

        @BeforeEach
        void setUp() {
            // TransactionTemplate.execute()가 콜백을 실행하도록 설정
            given(transactionTemplate.execute(any())).willAnswer(inv -> {
                        TransactionCallback<?> callback = inv.getArgument(0);
                        return callback.doInTransaction(null);
                    });
        }

        @Test
        @DisplayName("익명화 실행 — 30일 이전 기준으로 anonymizeDeletedUsers 호출")
        void anonymizeWithCorrectThreshold() {
            // given
            given(userRepository.anonymizeDeletedUsers(any())).willReturn(3);

            // when
            userScheduler.anonymizeDeletedUsers();

            // then — 기준일이 30일 이전인지 확인 (±1초 오차 허용)
            ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(userRepository).anonymizeDeletedUsers(captor.capture());

            LocalDateTime threshold = captor.getValue();
            assertThat(threshold).isBefore(LocalDateTime.now().minusDays(29));
            assertThat(threshold).isAfter(LocalDateTime.now().minusDays(31));
        }

        @Test
        @DisplayName("익명화 대상 없을 때 — 0건 처리")
        void anonymizeDeletedUsersWhenNone() {
            // given
            given(userRepository.anonymizeDeletedUsers(any())).willReturn(0);

            // when & then — 예외 없이 정상 처리
            userScheduler.anonymizeDeletedUsers();
            verify(userRepository).anonymizeDeletedUsers(any());
        }
    }

    @Nested
    @DisplayName("lastActiveAt 동기화 스케줄러")
    class SyncLastActiveAt {

        @Test
        @DisplayName("throttle 키는 스킵하고 lastActive 키만 처리")
        void skipThrottleKeys() {
            // given
            given(transactionTemplate.execute(any())).willAnswer(inv -> {
                        TransactionCallback<?> callback = inv.getArgument(0);
                        return callback.doInTransaction(null);
                    });
            Cursor<String> cursor = mock(Cursor.class);
            given(redisTemplate.scan(any(ScanOptions.class))).willReturn(cursor);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            // throttle 키 1개 + 일반 키 1개
            given(cursor.hasNext()).willReturn(true, true, false);
            given(cursor.next())
                    .willReturn("lastActive:throttle:1")
                    .willReturn("lastActive:2");

            given(valueOperations.getAndDelete("lastActive:2"))
                    .willReturn(Instant_now());

            // when
            userScheduler.syncLastActiveAt();

            // then — throttle 키는 getAndDelete 호출 안 됨
            verify(valueOperations, never()).getAndDelete("lastActive:throttle:1");
            verify(valueOperations).getAndDelete("lastActive:2");
        }

        @Test
        @DisplayName("Redis 값이 null이면 DB 업데이트 안 함")
        void skipNullValues() {
            // given
            Cursor<String> cursor = mock(Cursor.class);
            given(redisTemplate.scan(any(ScanOptions.class))).willReturn(cursor);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            given(cursor.hasNext()).willReturn(true, false);
            given(cursor.next()).willReturn("lastActive:1");
            given(valueOperations.getAndDelete("lastActive:1")).willReturn(null);

            // when
            userScheduler.syncLastActiveAt();

            // then
            verify(userRepository, never()).updateLastActiveAt(any(), any());
        }

        private String Instant_now() {
            return java.time.Instant.now().toString();
        }
    }

    @Nested
    @DisplayName("미접속 유저 정지 스케줄러")
    class SuspendInactiveUsers {

        @BeforeEach
        void setUp() {
            given(transactionTemplate.execute(any())).willAnswer(inv -> {
                TransactionCallback<?> callback = inv.getArgument(0);
                return callback.doInTransaction(null);
            });
        }

        @Test
        @DisplayName("30일 이전 기준으로 ACTIVE → SUSPENDED 처리")
        void suspendWithCorrectThreshold() {
            // given
            Cursor<String> cursor = mock(Cursor.class);
            given(redisTemplate.scan(any(ScanOptions.class))).willReturn(cursor);
            given(cursor.hasNext()).willReturn(false); // Redis 잔여 없음
            given(userRepository.suspendInactiveUsers(any(), any(), any())).willReturn(5);

            // when
            userScheduler.suspendInactiveUsers();

            // then
            ArgumentCaptor<LocalDateTime> thresholdCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<UserStatus> fromCaptor = ArgumentCaptor.forClass(UserStatus.class);
            ArgumentCaptor<UserStatus> toCaptor = ArgumentCaptor.forClass(UserStatus.class);

            verify(userRepository).suspendInactiveUsers(
                    thresholdCaptor.capture(), fromCaptor.capture(), toCaptor.capture());

            assertThat(thresholdCaptor.getValue()).isBefore(LocalDateTime.now().minusDays(29));
            assertThat(fromCaptor.getValue()).isEqualTo(UserStatus.ACTIVE);
            assertThat(toCaptor.getValue()).isEqualTo(UserStatus.SUSPENDED);
        }

        @Test
        @DisplayName("BANNED 유저는 fromStatus가 ACTIVE라 SUSPENDED 대상 아님")
        void bannedUserNotSuspended() {
            // given — suspendInactiveUsers는 fromStatus=ACTIVE만 처리
            Cursor<String> cursor = mock(Cursor.class);
            given(redisTemplate.scan(any(ScanOptions.class))).willReturn(cursor);
            given(cursor.hasNext()).willReturn(false);
            given(userRepository.suspendInactiveUsers(any(), any(), any())).willReturn(0);

            // when
            userScheduler.suspendInactiveUsers();

            // then — ACTIVE → SUSPENDED 만 처리 (BANNED 건드리지 않음)
            verify(userRepository).suspendInactiveUsers(
                    any(), eq(UserStatus.ACTIVE), eq(UserStatus.SUSPENDED));
        }

        @Test
        @DisplayName("정지 처리 전 Redis 잔여분 먼저 flush")
        void flushRedisBeforeSuspend() {
            // given
            Cursor<String> cursor = mock(Cursor.class);
            given(redisTemplate.scan(any(ScanOptions.class))).willReturn(cursor);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            // Redis에 lastActive 키 1개 존재
            given(cursor.hasNext()).willReturn(true, false);
            given(cursor.next()).willReturn("lastActive:1");
            given(valueOperations.getAndDelete("lastActive:1"))
                    .willReturn(java.time.Instant.now().toString());
            given(userRepository.suspendInactiveUsers(any(), any(), any())).willReturn(0);

            // when
            userScheduler.suspendInactiveUsers();

            // then — DB 업데이트 후 suspend 처리
            verify(userRepository).updateLastActiveAt(eq(1L), any());
            verify(userRepository).suspendInactiveUsers(any(), any(), any());
        }
    }
}
