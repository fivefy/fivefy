package com.fivefy.domain.user.scheduler;

import com.fivefy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserSchedulerTest {

    @InjectMocks
    private UserScheduler userScheduler;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("익명화 실행 — 30일 이전 기준으로 anonymizeDeletedUsers 호출")
    void anonymizeDeletedUsers() {
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