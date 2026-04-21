package com.fivefy.domain.playback.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.playback.dto.request.PlaybackPauseRequest;
import com.fivefy.domain.playback.dto.request.PlaybackPlayRequest;
import com.fivefy.domain.playback.dto.request.PlaybackSkipRequest;
import com.fivefy.domain.playback.dto.request.PlaybackStopRequest;
import com.fivefy.domain.playback.dto.response.PlaybackResponse;
import com.fivefy.domain.playback.entity.Playback;
import com.fivefy.domain.playback.enums.PlaybackErrorCode;
import com.fivefy.domain.playback.enums.PlaybackStatus;
import com.fivefy.domain.playback.repository.PlaybackRepository;
import com.fivefy.domain.playlisttrack.entity.PlaylistTrack;
import com.fivefy.domain.playlisttrack.repository.PlaylistTrackRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class PlaybackServiceTest {

    @InjectMocks
    private PlaybackService playbackService;

    @Mock private PlaybackRepository playbackRepository;
    @Mock private PlaylistTrackRepository playlistTrackRepository;

    @Nested
    @DisplayName("재생 시작")
    class Play {

        @Test
        @DisplayName("새로운 재생 시작 성공")
        void playSuccessNewPlayback() {
            // given
            Long userId = 1L;
            PlaybackPlayRequest request = new PlaybackPlayRequest(1L, 10L, "session-1", "device-1");

            Playback playback = Playback.create(
                    request.playlistId(),
                    request.trackId(),
                    userId,
                    request.sessionId(),
                    request.deviceId()
            );
            ReflectionTestUtils.setField(playback, "id", 1L);

            given(playlistTrackRepository.existsByPlaylistIdAndTrackId(1L, 10L)).willReturn(true);
            given(playbackRepository.findTopByUserIdAndSessionIdAndStatusOrderByIdDesc(
                    userId, "session-1", PlaybackStatus.PLAYING
            )).willReturn(Optional.empty());
            given(playbackRepository.findTopByUserIdAndPlaylistIdAndTrackIdAndSessionIdOrderByIdDesc(
                    userId, 1L, 10L, "session-1"
            )).willReturn(Optional.empty());
            given(playbackRepository.save(any(Playback.class))).willReturn(playback);

            // when
            PlaybackResponse result = playbackService.play(userId, request);

            // then
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.playlistId()).isEqualTo(1L);
            assertThat(result.trackId()).isEqualTo(10L);
            assertThat(result.userId()).isEqualTo(userId);
            assertThat(result.sessionId()).isEqualTo("session-1");
            assertThat(result.deviceId()).isEqualTo("device-1");
            assertThat(result.status()).isEqualTo(PlaybackStatus.PLAYING);
        }

        @Test
        @DisplayName("플레이리스트에 포함되지 않은 트랙이면 예외 발생")
        void playPlaylistTrackNotIncluded() {
            // given
            Long userId = 1L;
            PlaybackPlayRequest request = new PlaybackPlayRequest(1L, 10L, "session-1", "device-1");

            given(playlistTrackRepository.existsByPlaylistIdAndTrackId(1L, 10L)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> playbackService.play(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaybackErrorCode.PLAYLIST_TRACK_NOT_INCLUDED.getMessage());
        }

        @Test
        @DisplayName("일시정지 상태의 재생 기록이면 재개 성공")
        void playResumeSuccess() {
            // given
            Long userId = 1L;
            PlaybackPlayRequest request = new PlaybackPlayRequest(1L, 10L, "session-1", "device-1");

            Playback playback = Playback.create(1L, 10L, userId, "session-1", "device-1");
            ReflectionTestUtils.setField(playback, "id", 1L);
            ReflectionTestUtils.setField(playback, "status", PlaybackStatus.PAUSED);

            given(playlistTrackRepository.existsByPlaylistIdAndTrackId(1L, 10L)).willReturn(true);
            given(playbackRepository.findTopByUserIdAndSessionIdAndStatusOrderByIdDesc(
                    userId, "session-1", PlaybackStatus.PLAYING
            )).willReturn(Optional.empty());
            given(playbackRepository.findTopByUserIdAndPlaylistIdAndTrackIdAndSessionIdOrderByIdDesc(
                    userId, 1L, 10L, "session-1"
            )).willReturn(Optional.of(playback));
            given(playbackRepository.save(any(Playback.class))).willReturn(playback);

            // when
            PlaybackResponse result = playbackService.play(userId, request);

            // then
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.status()).isEqualTo(PlaybackStatus.PLAYING);
        }

        @Test
        @DisplayName("정지 상태의 재생 기록이면 새 재생 기록 생성 성공")
        void playStoppedPlaybackCreateNewSuccess() {
            // given
            Long userId = 1L;
            PlaybackPlayRequest request = new PlaybackPlayRequest(1L, 10L, "session-1", "device-1");

            Playback stoppedPlayback = Playback.create(1L, 10L, userId, "session-1", "device-1");
            ReflectionTestUtils.setField(stoppedPlayback, "id", 1L);
            ReflectionTestUtils.setField(stoppedPlayback, "status", PlaybackStatus.STOPPED);

            Playback newPlayback = Playback.create(1L, 10L, userId, "session-1", "device-1");
            ReflectionTestUtils.setField(newPlayback, "id", 2L);

            given(playlistTrackRepository.existsByPlaylistIdAndTrackId(1L, 10L)).willReturn(true);
            given(playbackRepository.findTopByUserIdAndSessionIdAndStatusOrderByIdDesc(
                    userId, "session-1", PlaybackStatus.PLAYING
            )).willReturn(Optional.empty());
            given(playbackRepository.findTopByUserIdAndPlaylistIdAndTrackIdAndSessionIdOrderByIdDesc(
                    userId, 1L, 10L, "session-1"
            )).willReturn(Optional.of(stoppedPlayback));
            given(playbackRepository.save(any(Playback.class))).willReturn(newPlayback);

            // when
            PlaybackResponse result = playbackService.play(userId, request);

            // then
            assertThat(result.id()).isEqualTo(2L);
            assertThat(result.status()).isEqualTo(PlaybackStatus.PLAYING);
        }

        @Test
        @DisplayName("완료 상태의 재생 기록이면 새 재생 기록 생성 성공")
        void playCompletedPlaybackCreateNewSuccess() {
            // given
            Long userId = 1L;
            PlaybackPlayRequest request = new PlaybackPlayRequest(1L, 10L, "session-1", "device-1");

            Playback completedPlayback = Playback.create(1L, 10L, userId, "session-1", "device-1");
            ReflectionTestUtils.setField(completedPlayback, "id", 1L);
            ReflectionTestUtils.setField(completedPlayback, "status", PlaybackStatus.COMPLETED);

            Playback newPlayback = Playback.create(1L, 10L, userId, "session-1", "device-1");
            ReflectionTestUtils.setField(newPlayback, "id", 2L);

            given(playlistTrackRepository.existsByPlaylistIdAndTrackId(1L, 10L)).willReturn(true);
            given(playbackRepository.findTopByUserIdAndSessionIdAndStatusOrderByIdDesc(
                    userId, "session-1", PlaybackStatus.PLAYING
            )).willReturn(Optional.empty());
            given(playbackRepository.findTopByUserIdAndPlaylistIdAndTrackIdAndSessionIdOrderByIdDesc(
                    userId, 1L, 10L, "session-1"
            )).willReturn(Optional.of(completedPlayback));
            given(playbackRepository.save(any(Playback.class))).willReturn(newPlayback);

            // when
            PlaybackResponse result = playbackService.play(userId, request);

            // then
            assertThat(result.id()).isEqualTo(2L);
            assertThat(result.status()).isEqualTo(PlaybackStatus.PLAYING);
        }

        @Test
        @DisplayName("건너뛴 상태의 재생 기록이면 새 재생 기록 생성 성공")
        void playSkippedPlaybackCreateNewSuccess() {
            // given
            Long userId = 1L;
            PlaybackPlayRequest request = new PlaybackPlayRequest(1L, 10L, "session-1", "device-1");

            Playback skippedPlayback = Playback.create(1L, 10L, userId, "session-1", "device-1");
            ReflectionTestUtils.setField(skippedPlayback, "id", 1L);
            ReflectionTestUtils.setField(skippedPlayback, "status", PlaybackStatus.SKIPPED);

            Playback newPlayback = Playback.create(1L, 10L, userId, "session-1", "device-1");
            ReflectionTestUtils.setField(newPlayback, "id", 2L);

            given(playlistTrackRepository.existsByPlaylistIdAndTrackId(1L, 10L)).willReturn(true);
            given(playbackRepository.findTopByUserIdAndSessionIdAndStatusOrderByIdDesc(
                    userId, "session-1", PlaybackStatus.PLAYING
            )).willReturn(Optional.empty());
            given(playbackRepository.findTopByUserIdAndPlaylistIdAndTrackIdAndSessionIdOrderByIdDesc(
                    userId, 1L, 10L, "session-1"
            )).willReturn(Optional.of(skippedPlayback));
            given(playbackRepository.save(any(Playback.class))).willReturn(newPlayback);

            // when
            PlaybackResponse result = playbackService.play(userId, request);

            // then
            assertThat(result.id()).isEqualTo(2L);
            assertThat(result.status()).isEqualTo(PlaybackStatus.PLAYING);
        }

        @Test
        @DisplayName("이미 같은 세션에서 같은 곡이 재생 중이면 예외 발생")
        void playAlreadyPlayingSameTrack() {
            // given
            Long userId = 1L;
            PlaybackPlayRequest request = new PlaybackPlayRequest(1L, 10L, "session-1", "device-1");

            Playback currentPlayback = Playback.create(1L, 10L, userId, "session-1", "device-1");
            ReflectionTestUtils.setField(currentPlayback, "id", 1L);

            given(playlistTrackRepository.existsByPlaylistIdAndTrackId(1L, 10L)).willReturn(true);
            given(playbackRepository.findTopByUserIdAndSessionIdAndStatusOrderByIdDesc(
                    userId, "session-1", PlaybackStatus.PLAYING
            )).willReturn(Optional.of(currentPlayback));

            // when & then
            assertThatThrownBy(() -> playbackService.play(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaybackErrorCode.INVALID_PLAYBACK_STATE.getMessage());
        }

        @Test
        @DisplayName("같은 세션에서 다른 곡이 재생 중이면 기존 곡 정지 후 새 곡 재생 성공")
        void playDifferentTrackWhilePlaying() {
            // given
            Long userId = 1L;
            PlaybackPlayRequest request = new PlaybackPlayRequest(1L, 20L, "session-1", "device-1");

            Playback currentPlayback = Playback.create(1L, 10L, userId, "session-1", "device-1");
            ReflectionTestUtils.setField(currentPlayback, "id", 1L);

            Playback newPlayback = Playback.create(1L, 20L, userId, "session-1", "device-1");
            ReflectionTestUtils.setField(newPlayback, "id", 2L);

            given(playlistTrackRepository.existsByPlaylistIdAndTrackId(1L, 20L)).willReturn(true);
            given(playbackRepository.findTopByUserIdAndSessionIdAndStatusOrderByIdDesc(
                    userId, "session-1", PlaybackStatus.PLAYING
            )).willReturn(Optional.of(currentPlayback));
            given(playbackRepository.findTopByUserIdAndPlaylistIdAndTrackIdAndSessionIdOrderByIdDesc(
                    userId, 1L, 20L, "session-1"
            )).willReturn(Optional.empty());
            given(playbackRepository.save(any(Playback.class))).willReturn(newPlayback);

            // when
            PlaybackResponse result = playbackService.play(userId, request);

            // then
            assertThat(currentPlayback.getStatus()).isEqualTo(PlaybackStatus.STOPPED);
            assertThat(result.id()).isEqualTo(2L);
            assertThat(result.trackId()).isEqualTo(20L);
            assertThat(result.status()).isEqualTo(PlaybackStatus.PLAYING);
        }

        @Test
        @DisplayName("기존 이력이 재생 중 상태면 새 재생 기록 생성")
        void playExistingHistoryPlayingCreateNewPlayback() {
            // given
            Long userId = 1L;
            PlaybackPlayRequest request = new PlaybackPlayRequest(1L, 10L, "session-1", "device-1");

            Playback existingPlayingPlayback = Playback.create(1L, 10L, userId, "session-1", "device-1");
            ReflectionTestUtils.setField(existingPlayingPlayback, "id", 1L);

            Playback newPlayback = Playback.create(1L, 10L, userId, "session-1", "device-1");
            ReflectionTestUtils.setField(newPlayback, "id", 2L);

            given(playlistTrackRepository.existsByPlaylistIdAndTrackId(1L, 10L)).willReturn(true);
            given(playbackRepository.findTopByUserIdAndSessionIdAndStatusOrderByIdDesc(
                    userId, "session-1", PlaybackStatus.PLAYING
            )).willReturn(Optional.empty());
            given(playbackRepository.findTopByUserIdAndPlaylistIdAndTrackIdAndSessionIdOrderByIdDesc(
                    userId, 1L, 10L, "session-1"
            )).willReturn(Optional.of(existingPlayingPlayback));
            given(playbackRepository.save(any(Playback.class))).willReturn(newPlayback);

            // when
            PlaybackResponse result = playbackService.play(userId, request);

            // then
            assertThat(result.id()).isEqualTo(2L);
            assertThat(result.status()).isEqualTo(PlaybackStatus.PLAYING);
        }

        @Test
        @DisplayName("같은 세션에서 같은 곡 재생 중이면 예외 발생")
        void playSameTrackWhilePlaying() {
            // given
            Long userId = 1L;
            PlaybackPlayRequest request = new PlaybackPlayRequest(1L, 10L, "session-1", "device-1");

            Playback currentPlayback = Playback.create(1L, 10L, userId, "session-1", "device-1");
            ReflectionTestUtils.setField(currentPlayback, "id", 1L);
            // create() 기본 상태 = PLAYING

            given(playlistTrackRepository.existsByPlaylistIdAndTrackId(1L, 10L)).willReturn(true);

            given(playbackRepository.findTopByUserIdAndSessionIdAndStatusOrderByIdDesc(
                    userId, "session-1", PlaybackStatus.PLAYING
            )).willReturn(Optional.of(currentPlayback));

            // when & then
            assertThatThrownBy(() ->
                    playbackService.play(userId, request)
            )
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaybackErrorCode.INVALID_PLAYBACK_STATE.getMessage());
        }
    }

    @Nested
    @DisplayName("재생 일시정지")
    class Pause {

        @Test
        @DisplayName("재생 일시정지 성공")
        void pauseSuccess() {
            // given
            Long userId = 1L;
            PlaybackPauseRequest request = new PlaybackPauseRequest(1L);

            Playback playback = Playback.create(1L, 10L, userId, "session-1", "device-1");
            ReflectionTestUtils.setField(playback, "id", 1L);

            given(playbackRepository.findByIdAndUserId(1L, userId)).willReturn(Optional.of(playback));

            // when
            PlaybackResponse result = playbackService.pause(userId, request);

            // then
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.status()).isEqualTo(PlaybackStatus.PAUSED);
        }

        @Test
        @DisplayName("존재하지 않는 재생 기록이면 예외 발생")
        void pausePlaybackNotFound() {
            // given
            Long userId = 1L;
            PlaybackPauseRequest request = new PlaybackPauseRequest(1L);

            given(playbackRepository.findByIdAndUserId(1L, userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> playbackService.pause(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaybackErrorCode.PLAYBACK_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("현재 재생 중인 음악이 아니면 예외 발생")
        void pauseCurrentPlaybackNotFound() {
            // given
            Long userId = 1L;
            PlaybackPauseRequest request = new PlaybackPauseRequest(1L);

            Playback playback = Playback.create(1L, 10L, userId, "session-1", "device-1");
            ReflectionTestUtils.setField(playback, "id", 1L);
            ReflectionTestUtils.setField(playback, "status", PlaybackStatus.PAUSED);

            given(playbackRepository.findByIdAndUserId(1L, userId)).willReturn(Optional.of(playback));

            // when & then
            assertThatThrownBy(() -> playbackService.pause(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaybackErrorCode.CURRENT_PLAYBACK_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("곡 정지")
    class Stop {

        @Test
        @DisplayName("재생 중인 곡 정지 성공")
        void stopPlayingSuccess() {
            // given
            Long userId = 1L;
            PlaybackStopRequest request = new PlaybackStopRequest(1L);

            Playback playback = Playback.create(1L, 10L, userId, "session-1", "device-1");
            ReflectionTestUtils.setField(playback, "id", 1L);

            given(playbackRepository.findByIdAndUserId(1L, userId)).willReturn(Optional.of(playback));

            // when
            PlaybackResponse result = playbackService.stop(userId, request);

            // then
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.status()).isEqualTo(PlaybackStatus.STOPPED);
            assertThat(result.endedAt()).isNotNull();
        }

        @Test
        @DisplayName("일시정지 상태의 곡 정지 성공")
        void stopPausedSuccess() {
            // given
            Long userId = 1L;
            PlaybackStopRequest request = new PlaybackStopRequest(1L);

            Playback playback = Playback.create(1L, 10L, userId, "session-1", "device-1");
            ReflectionTestUtils.setField(playback, "id", 1L);
            ReflectionTestUtils.setField(playback, "status", PlaybackStatus.PAUSED);

            given(playbackRepository.findByIdAndUserId(1L, userId)).willReturn(Optional.of(playback));

            // when
            PlaybackResponse result = playbackService.stop(userId, request);

            // then
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.status()).isEqualTo(PlaybackStatus.STOPPED);
            assertThat(result.endedAt()).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 재생 기록이면 예외 발생")
        void stopPlaybackNotFound() {
            // given
            Long userId = 1L;
            PlaybackStopRequest request = new PlaybackStopRequest(1L);

            given(playbackRepository.findByIdAndUserId(1L, userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> playbackService.stop(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaybackErrorCode.PLAYBACK_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("정지할 수 없는 상태면 예외 발생")
        void stopInvalidState() {
            // given
            Long userId = 1L;
            PlaybackStopRequest request = new PlaybackStopRequest(1L);

            Playback playback = Playback.create(1L, 10L, userId, "session-1", "device-1");
            ReflectionTestUtils.setField(playback, "id", 1L);
            ReflectionTestUtils.setField(playback, "status", PlaybackStatus.STOPPED);

            given(playbackRepository.findByIdAndUserId(1L, userId)).willReturn(Optional.of(playback));

            // when & then
            assertThatThrownBy(() -> playbackService.stop(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaybackErrorCode.INVALID_PLAYBACK_STATE.getMessage());
        }
    }

    @Nested
    @DisplayName("곡 건너뛰기")
    class Skip {

        @Test
        @DisplayName("곡 건너뛰기 성공")
        void skipSuccess() {
            // given
            Long userId = 1L;
            PlaybackSkipRequest request = new PlaybackSkipRequest(1L);

            Playback currentPlayback = Playback.create(1L, 10L, userId, "session-1", "device-1");
            ReflectionTestUtils.setField(currentPlayback, "id", 1L);

            PlaylistTrack currentTrack = mock(PlaylistTrack.class);
            PlaylistTrack nextTrack = mock(PlaylistTrack.class);

            given(currentTrack.getTrackId()).willReturn(10L);
            given(nextTrack.getTrackId()).willReturn(20L);

            Playback nextPlayback = Playback.create(1L, 20L, userId, "session-1", "device-1");
            ReflectionTestUtils.setField(nextPlayback, "id", 2L);

            given(playbackRepository.findByIdAndUserId(1L, userId)).willReturn(Optional.of(currentPlayback));
            given(playlistTrackRepository.findAllByPlaylistIdOrderByPositionAsc(1L))
                    .willReturn(List.of(currentTrack, nextTrack));
            given(playbackRepository.save(any(Playback.class))).willReturn(nextPlayback);

            // when
            PlaybackResponse result = playbackService.skip(userId, request);

            // then
            assertThat(result.id()).isEqualTo(2L);
            assertThat(result.trackId()).isEqualTo(20L);
            assertThat(result.status()).isEqualTo(PlaybackStatus.PLAYING);
        }

        @Test
        @DisplayName("마지막 곡에서 건너뛰면 처음 곡으로 순환")
        void skipWrapAroundSuccess() {
            // given
            Long userId = 1L;
            PlaybackSkipRequest request = new PlaybackSkipRequest(1L);

            Playback currentPlayback = Playback.create(1L, 20L, userId, "session-1", "device-1");
            ReflectionTestUtils.setField(currentPlayback, "id", 1L);

            PlaylistTrack firstTrack = mock(PlaylistTrack.class);
            PlaylistTrack lastTrack = mock(PlaylistTrack.class);

            given(firstTrack.getTrackId()).willReturn(10L);
            given(lastTrack.getTrackId()).willReturn(20L);

            Playback nextPlayback = Playback.create(1L, 10L, userId, "session-1", "device-1");
            ReflectionTestUtils.setField(nextPlayback, "id", 2L);

            given(playbackRepository.findByIdAndUserId(1L, userId)).willReturn(Optional.of(currentPlayback));
            given(playlistTrackRepository.findAllByPlaylistIdOrderByPositionAsc(1L))
                    .willReturn(List.of(firstTrack, lastTrack));
            given(playbackRepository.save(any(Playback.class))).willReturn(nextPlayback);

            // when
            PlaybackResponse result = playbackService.skip(userId, request);

            // then
            assertThat(result.trackId()).isEqualTo(10L);
            assertThat(result.status()).isEqualTo(PlaybackStatus.PLAYING);
        }

        @Test
        @DisplayName("존재하지 않는 재생 기록이면 예외 발생")
        void skipPlaybackNotFound() {
            // given
            Long userId = 1L;
            PlaybackSkipRequest request = new PlaybackSkipRequest(1L);

            given(playbackRepository.findByIdAndUserId(1L, userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> playbackService.skip(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaybackErrorCode.PLAYBACK_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("건너뛸 수 없는 상태면 예외 발생")
        void skipInvalidState() {
            // given
            Long userId = 1L;
            PlaybackSkipRequest request = new PlaybackSkipRequest(1L);

            Playback currentPlayback = Playback.create(1L, 10L, userId, "session-1", "device-1");
            ReflectionTestUtils.setField(currentPlayback, "id", 1L);
            ReflectionTestUtils.setField(currentPlayback, "status", PlaybackStatus.STOPPED);

            given(playbackRepository.findByIdAndUserId(1L, userId)).willReturn(Optional.of(currentPlayback));

            // when & then
            assertThatThrownBy(() -> playbackService.skip(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaybackErrorCode.INVALID_PLAYBACK_STATE.getMessage());
        }

        @Test
        @DisplayName("플레이리스트 트랙 정보가 없으면 예외 발생")
        void skipPlaylistTrackNotFound() {
            // given
            Long userId = 1L;
            PlaybackSkipRequest request = new PlaybackSkipRequest(1L);

            Playback currentPlayback = Playback.create(1L, 10L, userId, "session-1", "device-1");
            ReflectionTestUtils.setField(currentPlayback, "id", 1L);

            given(playbackRepository.findByIdAndUserId(1L, userId)).willReturn(Optional.of(currentPlayback));
            given(playlistTrackRepository.findAllByPlaylistIdOrderByPositionAsc(1L))
                    .willReturn(List.of());

            // when & then
            assertThatThrownBy(() -> playbackService.skip(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaybackErrorCode.PLAYLIST_TRACK_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("현재 트랙이 플레이리스트 트랙 순서와 일치하지 않으면 예외 발생")
        void skipTrackMismatch() {
            // given
            Long userId = 1L;
            PlaybackSkipRequest request = new PlaybackSkipRequest(1L);

            Playback currentPlayback = Playback.create(1L, 999L, userId, "session-1", "device-1");
            ReflectionTestUtils.setField(currentPlayback, "id", 1L);

            PlaylistTrack track1 = mock(PlaylistTrack.class);
            PlaylistTrack track2 = mock(PlaylistTrack.class);

            given(track1.getTrackId()).willReturn(10L);
            given(track2.getTrackId()).willReturn(20L);

            given(playbackRepository.findByIdAndUserId(1L, userId)).willReturn(Optional.of(currentPlayback));
            given(playlistTrackRepository.findAllByPlaylistIdOrderByPositionAsc(1L))
                    .willReturn(List.of(track1, track2));

            // when & then
            assertThatThrownBy(() -> playbackService.skip(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(PlaybackErrorCode.PLAYBACK_TRACK_MISMATCH.getMessage());
        }

        @Test
        @DisplayName("일시정지 상태에서 곡 건너뛰기 성공")
        void skipPausedSuccess() {
            // given
            Long userId = 1L;
            PlaybackSkipRequest request = new PlaybackSkipRequest(1L);

            Playback currentPlayback = Playback.create(1L, 10L, userId, "session-1", "device-1");
            ReflectionTestUtils.setField(currentPlayback, "id", 1L);
            ReflectionTestUtils.setField(currentPlayback, "status", PlaybackStatus.PAUSED);

            PlaylistTrack currentTrack = mock(PlaylistTrack.class);
            PlaylistTrack nextTrack = mock(PlaylistTrack.class);

            given(currentTrack.getTrackId()).willReturn(10L);
            given(nextTrack.getTrackId()).willReturn(20L);

            Playback nextPlayback = Playback.create(1L, 20L, userId, "session-1", "device-1");
            ReflectionTestUtils.setField(nextPlayback, "id", 2L);

            given(playbackRepository.findByIdAndUserId(1L, userId)).willReturn(Optional.of(currentPlayback));
            given(playlistTrackRepository.findAllByPlaylistIdOrderByPositionAsc(1L))
                    .willReturn(List.of(currentTrack, nextTrack));
            given(playbackRepository.save(any(Playback.class))).willReturn(nextPlayback);

            // when
            PlaybackResponse result = playbackService.skip(userId, request);

            // then
            assertThat(result.id()).isEqualTo(2L);
            assertThat(result.trackId()).isEqualTo(20L);
            assertThat(result.status()).isEqualTo(PlaybackStatus.PLAYING);
        }
    }

    @Nested
    @DisplayName("재생 기록 조회")
    class GetPlaybackHistory {

        @Test
        @DisplayName("재생 기록 조회 성공")
        void getPlaybackHistorySuccess() {
            // given
            Long userId = 1L;

            Playback playback1 = Playback.create(1L, 20L, userId, "session-1", "device-1");
            Playback playback2 = Playback.create(1L, 10L, userId, "session-1", "device-1");

            ReflectionTestUtils.setField(playback1, "id", 2L);
            ReflectionTestUtils.setField(playback2, "id", 1L);
            ReflectionTestUtils.setField(playback1, "status", PlaybackStatus.SKIPPED);
            ReflectionTestUtils.setField(playback2, "status", PlaybackStatus.COMPLETED);

            given(playbackRepository.findAllByUserIdOrderByIdDesc(userId))
                    .willReturn(List.of(playback1, playback2));

            // when
            List<PlaybackResponse> result = playbackService.getPlaybackHistory(userId);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).id()).isEqualTo(2L);
            assertThat(result.get(0).status()).isEqualTo(PlaybackStatus.SKIPPED);
            assertThat(result.get(1).id()).isEqualTo(1L);
            assertThat(result.get(1).status()).isEqualTo(PlaybackStatus.COMPLETED);
        }
    }
}
