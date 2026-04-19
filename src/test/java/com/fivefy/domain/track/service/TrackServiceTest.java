package com.fivefy.domain.track.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.track.dto.request.FreeTrackApplicationCreateRequest;
import com.fivefy.domain.track.dto.response.TrackApplicationResponse;
import com.fivefy.domain.track.entity.TrackApplication;
import com.fivefy.domain.track.enums.TrackApplicationErrorCode;
import com.fivefy.domain.track.enums.TrackType;
import com.fivefy.domain.track.repository.TrackApplicationRepository;
import com.fivefy.domain.user.entity.User;
import com.fivefy.domain.user.enums.UserErrorCode;
import com.fivefy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * TrackService의 비즈니스 로직을 검증하는 단위 테스트
 *
 * 자유 창작 트랙 등록 신청 생성 기능을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class TrackServiceTest {

    @Mock
    private TrackApplicationRepository trackApplicationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TrackService trackService;

    @Nested
    @DisplayName("자유 창작 트랙 등록 신청 생성")
    class CreateFreeTrackApplication {

        @Test
        @DisplayName("자유 창작 트랙 등록 신청 성공")
        void createFreeTrackApplication_success() {
            Long userId = 1L;

            FreeTrackApplicationCreateRequest request =
                    new FreeTrackApplicationCreateRequest(
                            "밤편지 AI 버전",
                            "가사",
                            "BALLAD",
                            "https://example.com/audio.mp3",
                            210L
                    );

            User user = mock(User.class);
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

            when(trackApplicationRepository.existsPendingFreeCreationApplication(
                    userId,
                    request.title(),
                    request.audioUrl()
            )).thenReturn(false);

            TrackApplication savedApplication = TrackApplication.create(
                    userId,
                    TrackType.FREE_CREATION,
                    null,
                    null,
                    null,
                    request.title(),
                    request.lyrics(),
                    request.genre(),
                    request.audioUrl(),
                    request.durationSec(),
                    null,
                    null
            );
            ReflectionTestUtils.setField(savedApplication, "id", 1L);
            ReflectionTestUtils.setField(
                    savedApplication,
                    "createdAt",
                    LocalDateTime.of(2026, 4, 19, 20, 0, 0)
            );

            when(trackApplicationRepository.save(any(TrackApplication.class)))
                    .thenReturn(savedApplication);

            TrackApplicationResponse response =
                    trackService.createFreeTrackApplication(userId, request);

            assertThat(response.applicationId()).isEqualTo(1L);
            assertThat(response.trackType()).isEqualTo(TrackType.FREE_CREATION);
            assertThat(response.artistId()).isNull();
            assertThat(response.albumId()).isNull();
            assertThat(response.title()).isEqualTo("밤편지 AI 버전");
            assertThat(response.status()).isEqualTo(savedApplication.getStatus());
            assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2026, 4, 19, 20, 0, 0));
        }

        @Test
        @DisplayName("존재하지 않는 유저면 자유 창작 트랙 등록 신청 생성 실패")
        void createFreeTrackApplication_fail_whenUserNotFound() {
            Long userId = 1L;

            FreeTrackApplicationCreateRequest request =
                    new FreeTrackApplicationCreateRequest(
                            "밤편지 AI 버전",
                            "가사",
                            "BALLAD",
                            "https://example.com/audio.mp3",
                            210L
                    );

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> trackService.createFreeTrackApplication(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(UserErrorCode.ERR_USER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("동일한 진행 중 신청이 이미 있으면 자유 창작 트랙 등록 신청 생성 실패")
        void createFreeTrackApplication_fail_whenAlreadyExists() {
            Long userId = 1L;

            FreeTrackApplicationCreateRequest request =
                    new FreeTrackApplicationCreateRequest(
                            "밤편지 AI 버전",
                            "가사",
                            "BALLAD",
                            "https://example.com/audio.mp3",
                            210L
                    );

            User user = mock(User.class);
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

            when(trackApplicationRepository.existsPendingFreeCreationApplication(
                    userId,
                    request.title(),
                    request.audioUrl()
            )).thenReturn(true);

            assertThatThrownBy(() -> trackService.createFreeTrackApplication(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackApplicationErrorCode.ERR_TRACK_APPLICATION_ALREADY_EXISTS.getMessage());
        }
    }
}