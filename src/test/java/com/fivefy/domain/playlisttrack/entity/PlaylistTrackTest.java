package com.fivefy.domain.playlisttrack.entity;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.playlist.enums.PlaylistErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaylistTrackTest {

    @Test
    @DisplayName("플레이리스트 트랙 생성 성공")
    void create_success() {
        // given & when
        PlaylistTrack track = PlaylistTrack.create(1L, 10L, 1);

        // then
        assertThat(track.getPlaylistId()).isEqualTo(1L);
        assertThat(track.getTrackId()).isEqualTo(10L);
        assertThat(track.getPosition()).isEqualTo(1);
    }

    @Test
    @DisplayName("플레이리스트 트랙 생성 실패 - 유효하지 않은 위치")
    void create_invalid_position() {
        // when & then
        assertThatThrownBy(() -> PlaylistTrack.create(1L, 10L, 0))
                .isInstanceOf(BusinessException.class)
                .hasMessage(PlaylistErrorCode.INVALID_POSITION.getMessage());
    }

    @Test
    @DisplayName("트랙 순서 수정 성공")
    void updatePosition_success() {
        // given
        PlaylistTrack track = PlaylistTrack.create(1L, 10L, 1);

        // when
        track.updatePosition(2);

        // then
        assertThat(track.getPosition()).isEqualTo(2);
    }

    @Test
    @DisplayName("트랙 순서 수정 실패 - 유효하지 않은 위치")
    void updatePosition_invalid() {
        // given
        PlaylistTrack track = PlaylistTrack.create(1L, 10L, 1);

        // when & then
        assertThatThrownBy(() -> track.updatePosition(0))
                .isInstanceOf(BusinessException.class)
                .hasMessage(PlaylistErrorCode.INVALID_POSITION.getMessage());
    }

    @Test
    @DisplayName("트랙 임시 순서 이동")
    void moveToTemporaryPosition() {
        // given
        PlaylistTrack track = PlaylistTrack.create(1L, 10L, 1);

        // when
        track.moveToTemporaryPosition(-1);

        // then
        assertThat(track.getPosition()).isEqualTo(-1);
    }
}
