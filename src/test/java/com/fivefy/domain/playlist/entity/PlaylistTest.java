package com.fivefy.domain.playlist.entity;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.playlist.enums.PlaylistErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaylistTest {

    @Test
    @DisplayName("플레이리스트 생성 성공")
    void create_success() {
        // given
        Long userId = 1L;
        String title = "title";
        String description = "desc";

        // when
        Playlist playlist = Playlist.create(userId, title, description);

        // then
        assertThat(playlist.getUserId()).isEqualTo(userId);
        assertThat(playlist.getTitle()).isEqualTo(title);
        assertThat(playlist.getDescription()).isEqualTo(description);
        assertThat(playlist.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("플레이리스트 생성 시 제목이 null이면 예외 발생")
    void create_invalid_title_null() {
        // when & then
        assertThatThrownBy(() ->
                Playlist.create(1L, null, "desc")
        )
                .isInstanceOf(BusinessException.class)
                .hasMessage(PlaylistErrorCode.INVALID_TITLE.getMessage());
    }

    @Test
    @DisplayName("플레이리스트 생성 시 제목이 blank면 예외 발생")
    void create_invalid_title_blank() {
        // when & then
        assertThatThrownBy(() ->
                Playlist.create(1L, "", "desc")
        )
                .isInstanceOf(BusinessException.class)
                .hasMessage(PlaylistErrorCode.INVALID_TITLE.getMessage());
    }

    @Test
    @DisplayName("플레이리스트 생성 시 제목이 100자를 초과하면 예외 발생")
    void create_invalid_title_too_long() {
        // given
        String tooLongTitle = "a".repeat(101);

        // when & then
        assertThatThrownBy(() ->
                Playlist.create(1L, tooLongTitle, "desc")
        )
                .isInstanceOf(BusinessException.class)
                .hasMessage(PlaylistErrorCode.INVALID_TITLE.getMessage());
    }

    @Test
    @DisplayName("플레이리스트 수정 성공")
    void update_success() {
        // given
        Playlist playlist = Playlist.create(1L, "title", "desc");

        // when
        playlist.update("new", "newDesc");

        // then
        assertThat(playlist.getTitle()).isEqualTo("new");
        assertThat(playlist.getDescription()).isEqualTo("newDesc");
    }

    @Test
    @DisplayName("플레이리스트 수정 시 제목이 null이면 예외 발생")
    void update_invalid_title_null() {
        // given
        Playlist playlist = Playlist.create(1L, "title", "desc");

        // when & then
        assertThatThrownBy(() ->
                playlist.update(null, "desc")
        )
                .isInstanceOf(BusinessException.class)
                .hasMessage(PlaylistErrorCode.INVALID_TITLE.getMessage());
    }

    @Test
    @DisplayName("플레이리스트 수정 시 제목이 blank면 예외 발생")
    void update_invalid_title_blank() {
        // given
        Playlist playlist = Playlist.create(1L, "title", "desc");

        // when & then
        assertThatThrownBy(() ->
                playlist.update("", "desc")
        )
                .isInstanceOf(BusinessException.class)
                .hasMessage(PlaylistErrorCode.INVALID_TITLE.getMessage());
    }

    @Test
    @DisplayName("플레이리스트 수정 시 제목이 100자를 초과하면 예외 발생")
    void update_invalid_title_too_long() {
        // given
        Playlist playlist = Playlist.create(1L, "title", "desc");
        String tooLongTitle = "a".repeat(101);

        // when & then
        assertThatThrownBy(() ->
                playlist.update(tooLongTitle, "desc")
        )
                .isInstanceOf(BusinessException.class)
                .hasMessage(PlaylistErrorCode.INVALID_TITLE.getMessage());
    }

    @Test
    @DisplayName("플레이리스트 삭제 성공")
    void delete_success() {
        // given
        Playlist playlist = Playlist.create(1L, "title", "desc");

        // when
        playlist.delete();

        // then
        assertThat(playlist.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 삭제된 플레이리스트를 다시 삭제하면 예외 발생")
    void delete_already_deleted() {
        // given
        Playlist playlist = Playlist.create(1L, "title", "desc");
        playlist.delete();

        // when & then
        assertThatThrownBy(playlist::delete)
                .isInstanceOf(BusinessException.class)
                .hasMessage(PlaylistErrorCode.ALREADY_DELETED_PLAYLIST.getMessage());
    }
}
