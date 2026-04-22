package com.fivefy.domain.playlist.service;

import com.fivefy.domain.playlist.repository.PlaylistRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlaylistCleanupServiceTest {

    @InjectMocks
    private PlaylistCleanupService playlistCleanupService;

    @Mock private PlaylistRepository playlistRepository;

    @Test
    @DisplayName("기준 시각 이전의 soft delete 데이터 정리 성공")
    void cleanupDeletedPlaylists_success() {
        // given
        LocalDateTime threshold = LocalDateTime.of(2026, 4, 1, 0, 0);
        given(playlistRepository.deleteAllSoftDeletedBefore(threshold)).willReturn(3);

        // when
        int deletedCount = playlistCleanupService.cleanupDeletedPlaylists(threshold);

        // then
        assertThat(deletedCount).isEqualTo(3);
        verify(playlistRepository).deleteAllSoftDeletedBefore(threshold);
    }
}
