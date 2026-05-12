package com.fivefy.domain.playback.entity;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.playback.enums.PlaybackErrorCode;
import com.fivefy.domain.playback.enums.PlaybackStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaybackTest {

    @Test
    @DisplayName("재생 생성 성공")
    void create_success() {
        // given
        Long playlistId = 1L;
        Long trackId = 10L;
        Long userId = 100L;
        String sessionId = "session-1";
        String deviceId = "device-1";

        // when
        Playback playback = Playback.create(playlistId, trackId, userId, sessionId, deviceId);

        // then
        assertThat(playback.getPlaylistId()).isEqualTo(playlistId);
        assertThat(playback.getTrackId()).isEqualTo(trackId);
        assertThat(playback.getUserId()).isEqualTo(userId);
        assertThat(playback.getSessionId()).isEqualTo(sessionId);
        assertThat(playback.getDeviceId()).isEqualTo(deviceId);
        assertThat(playback.getStatus()).isEqualTo(PlaybackStatus.PLAYING);
        assertThat(playback.getPlayedDuration()).isEqualTo(0);
        assertThat(playback.getStartedAt()).isNotNull();
        assertThat(playback.getLastPlayedAt()).isNotNull();
        assertThat(playback.getEndedAt()).isNull();
    }

    @Test
    @DisplayName("재생 생성 시 playlistId가 null이면 예외 발생")
    void create_invalid_playlistId_null() {
        // given & when & then
        assertThatThrownBy(() ->
                Playback.create(null, 10L, 1L, "session-1", "device-1")
        ).isInstanceOf(NullPointerException.class)
                .hasMessage("playlistId(은)는 필수입니다");
    }

    @Test
    @DisplayName("재생 생성 시 trackId가 null이면 예외 발생")
    void create_invalid_trackId_null() {
        // given & when & then
        assertThatThrownBy(() ->
                Playback.create(1L, null, 1L, "session-1", "device-1")
        ).isInstanceOf(NullPointerException.class)
                .hasMessage("trackId(은)는 필수입니다");
    }

    @Test
    @DisplayName("재생 생성 시 userId가 null이면 예외 발생")
    void create_invalid_userId_null() {
        // given & when & then
        assertThatThrownBy(() ->
                Playback.create(1L, 10L, null, "session-1", "device-1")
        ).isInstanceOf(NullPointerException.class)
                .hasMessage("userId(은)는 필수입니다");
    }

    @Test
    @DisplayName("재생 생성 시 sessionId가 null이면 예외 발생")
    void create_invalid_sessionId_null() {
        // given & when & then
        assertThatThrownBy(() ->
                Playback.create(1L, 10L, 1L, null, "device-1")
        ).isInstanceOf(NullPointerException.class)
                .hasMessage("sessionId(은)는 필수입니다");
    }

    @Test
    @DisplayName("본인 재생 기록이면 true 반환")
    void isOwner_true() {
        // given
        Playback playback = Playback.create(1L, 10L, 1L, "session-1", "device-1");

        // when
        boolean result = playback.isOwner(1L);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("본인 재생 기록이 아니면 false 반환")
    void isOwner_false() {
        // given
        Playback playback = Playback.create(1L, 10L, 1L, "session-1", "device-1");

        // when
        boolean result = playback.isOwner(2L);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("재생 중 상태 확인")
    void isPlaying_true() {
        // given
        Playback playback = Playback.create(1L, 10L, 1L, "session-1", "device-1");

        // when & then
        assertThat(playback.isPlaying()).isTrue();
        assertThat(playback.isPaused()).isFalse();
        assertThat(playback.isStopped()).isFalse();
        assertThat(playback.isSkipped()).isFalse();
        assertThat(playback.isCompleted()).isFalse();
    }

    @Test
    @DisplayName("일시정지 상태 확인")
    void isPaused_true() {
        // given
        Playback playback = Playback.create(1L, 10L, 1L, "session-1", "device-1");
        ReflectionTestUtils.setField(playback, "status", PlaybackStatus.PAUSED);

        // when & then
        assertThat(playback.isPaused()).isTrue();
        assertThat(playback.isPlaying()).isFalse();
    }

    @Test
    @DisplayName("정지 상태 확인")
    void isStopped_true() {
        // given
        Playback playback = Playback.create(1L, 10L, 1L, "session-1", "device-1");
        ReflectionTestUtils.setField(playback, "status", PlaybackStatus.STOPPED);

        // when & then
        assertThat(playback.isStopped()).isTrue();
    }

    @Test
    @DisplayName("건너뛴 상태 확인")
    void isSkipped_true() {
        // given
        Playback playback = Playback.create(1L, 10L, 1L, "session-1", "device-1");
        ReflectionTestUtils.setField(playback, "status", PlaybackStatus.SKIPPED);

        // when & then
        assertThat(playback.isSkipped()).isTrue();
    }

    @Test
    @DisplayName("재생 완료 상태 확인")
    void isCompleted_true() {
        // given
        Playback playback = Playback.create(1L, 10L, 1L, "session-1", "device-1");
        ReflectionTestUtils.setField(playback, "status", PlaybackStatus.COMPLETED);

        // when & then
        assertThat(playback.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("일시정지 상태에서 재개 성공")
    void resume_success() {
        // given
        Playback playback = Playback.create(1L, 10L, 1L, "session-1", "device-1");
        ReflectionTestUtils.setField(playback, "status", PlaybackStatus.PAUSED);

        // when
        playback.resume();

        // then
        assertThat(playback.getStatus()).isEqualTo(PlaybackStatus.PLAYING);
    }

    @Test
    @DisplayName("일시정지 상태가 아니면 재개 시 예외 발생")
    void resume_invalid_state() {
        // given
        Playback playback = Playback.create(1L, 10L, 1L, "session-1", "device-1");

        // when & then
        assertThatThrownBy(playback::resume)
                .isInstanceOf(BusinessException.class)
                .hasMessage(PlaybackErrorCode.INVALID_PLAYBACK_STATE.getMessage());
    }

    @Test
    @DisplayName("재생 중인 곡 일시정지 성공")
    void pause_success() {
        // given
        Playback playback = Playback.create(1L, 10L, 1L, "session-1", "device-1");
        ReflectionTestUtils.setField(playback, "lastPlayedAt", LocalDateTime.now().minusSeconds(5));

        // when
        playback.pause(30);

        // then
        assertThat(playback.getStatus()).isEqualTo(PlaybackStatus.PAUSED);
        assertThat(playback.getPlayedDuration()).isGreaterThan(0);
    }

    @Test
    @DisplayName("재생 중이 아니면 일시정지 시 예외 발생")
    void pause_invalid_state() {
        // given
        Playback playback = Playback.create(1L, 10L, 1L, "session-1", "device-1");
        ReflectionTestUtils.setField(playback, "status", PlaybackStatus.PAUSED);

        // when & then
        assertThatThrownBy(() -> playback.pause(30))
                .isInstanceOf(BusinessException.class)
                .hasMessage(PlaybackErrorCode.INVALID_PLAYBACK_STATE.getMessage());
    }

    @Test
    @DisplayName("재생 중인 곡 정지 성공")
    void stop_playing_success() {
        // given
        Playback playback = Playback.create(1L, 10L, 1L, "session-1", "device-1");
        ReflectionTestUtils.setField(playback, "lastPlayedAt", LocalDateTime.now().minusSeconds(5));

        // when
        playback.stop(30);

        // then
        assertThat(playback.getStatus()).isEqualTo(PlaybackStatus.STOPPED);
        assertThat(playback.getEndedAt()).isNotNull();
        assertThat(playback.getPlayedDuration()).isGreaterThan(0);
    }

    @Test
    @DisplayName("일시정지 상태의 곡 정지 성공")
    void stop_paused_success() {
        // given
        Playback playback = Playback.create(1L, 10L, 1L, "session-1", "device-1");
        ReflectionTestUtils.setField(playback, "status", PlaybackStatus.PAUSED);

        // when
        playback.stop(30);

        // then
        assertThat(playback.getStatus()).isEqualTo(PlaybackStatus.STOPPED);
        assertThat(playback.getEndedAt()).isNotNull();
    }

    @Test
    @DisplayName("정지할 수 없는 상태면 예외 발생")
    void stop_invalid_state() {
        // given
        Playback playback = Playback.create(1L, 10L, 1L, "session-1", "device-1");
        ReflectionTestUtils.setField(playback, "status", PlaybackStatus.STOPPED);

        // when & then
        assertThatThrownBy(() -> playback.stop(30))
                .isInstanceOf(BusinessException.class)
                .hasMessage(PlaybackErrorCode.INVALID_PLAYBACK_STATE.getMessage());
    }

    @Test
    @DisplayName("재생 중인 곡 건너뛰기 성공")
    void skip_playing_success() {
        // given
        Playback playback = Playback.create(1L, 10L, 1L, "session-1", "device-1");
        ReflectionTestUtils.setField(playback, "lastPlayedAt", LocalDateTime.now().minusSeconds(5));

        // when
        playback.skip(30);

        // then
        assertThat(playback.getStatus()).isEqualTo(PlaybackStatus.SKIPPED);
        assertThat(playback.getEndedAt()).isNotNull();
        assertThat(playback.getPlayedDuration()).isGreaterThan(0);
    }

    @Test
    @DisplayName("일시정지 상태의 곡 건너뛰기 성공")
    void skip_paused_success() {
        // given
        Playback playback = Playback.create(1L, 10L, 1L, "session-1", "device-1");
        ReflectionTestUtils.setField(playback, "status", PlaybackStatus.PAUSED);

        // when
        playback.skip(30);

        // then
        assertThat(playback.getStatus()).isEqualTo(PlaybackStatus.SKIPPED);
        assertThat(playback.getEndedAt()).isNotNull();
    }

    @Test
    @DisplayName("건너뛸 수 없는 상태면 예외 발생")
    void skip_invalid_state() {
        // given
        Playback playback = Playback.create(1L, 10L, 1L, "session-1", "device-1");
        ReflectionTestUtils.setField(playback, "status", PlaybackStatus.STOPPED);

        // when & then
        assertThatThrownBy(() -> playback.skip(30))
                .isInstanceOf(BusinessException.class)
                .hasMessage(PlaybackErrorCode.INVALID_PLAYBACK_STATE.getMessage());
    }

    @Test
    @DisplayName("재생 완료 성공")
    void complete_success() {
        // given
        Playback playback = Playback.create(1L, 10L, 1L, "session-1", "device-1");
        ReflectionTestUtils.setField(playback, "lastPlayedAt", LocalDateTime.now().minusSeconds(5));

        // when
        playback.complete();

        // then
        assertThat(playback.getStatus()).isEqualTo(PlaybackStatus.COMPLETED);
        assertThat(playback.getEndedAt()).isNotNull();
        assertThat(playback.getPlayedDuration()).isGreaterThan(0);
    }

    @Test
    @DisplayName("재생 중이 아니면 완료 처리 시 예외 발생")
    void complete_invalid_state() {
        // given
        Playback playback = Playback.create(1L, 10L, 1L, "session-1", "device-1");
        ReflectionTestUtils.setField(playback, "status", PlaybackStatus.PAUSED);

        // when & then
        assertThatThrownBy(playback::complete)
                .isInstanceOf(BusinessException.class)
                .hasMessage(PlaybackErrorCode.INVALID_PLAYBACK_STATE.getMessage());
    }

    @Test
    @DisplayName("재생 시간 누적 시 시간이 증가하지 않았으면 playedDuration을 증가시키지 않는다")
    void accumulatePlayedDuration_zero_seconds() {
        // given
        Playback playback = Playback.create(1L, 10L, 1L, "session-1", "device-1");
        ReflectionTestUtils.setField(playback, "lastPlayedAt", LocalDateTime.now().plusSeconds(5));

        // when
        playback.complete();

        // then
        assertThat(playback.getStatus()).isEqualTo(PlaybackStatus.COMPLETED);
        assertThat(playback.getPlayedDuration()).isEqualTo(0);
    }

    @Test
    @DisplayName("playedDuration이 음수이면 예외 발생")
    void pause_invalid_playedDuration_negative() {
        // given
        Playback playback = Playback.create(1L, 10L, 1L, "session-1", "device-1");

        // when & then
        assertThatThrownBy(() -> playback.pause(-1))
                .isInstanceOf(BusinessException.class)
                .hasMessage(PlaybackErrorCode.INVALID_PLAYBACK_STATE.getMessage());
    }

    @Test
    @DisplayName("playedDuration이 null이면 예외 발생")
    void pause_invalid_playedDuration_null() {
        // given
        Playback playback = Playback.create(1L, 10L, 1L, "session-1", "device-1");

        // when & then
        assertThatThrownBy(() -> playback.pause(null))
                .isInstanceOf(BusinessException.class)
                .hasMessage(PlaybackErrorCode.INVALID_PLAYBACK_STATE.getMessage());
    }
}
