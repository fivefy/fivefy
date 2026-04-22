package com.fivefy.domain.playlist.scheduler;

import com.fivefy.domain.playlist.service.PlaylistCleanupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlaylistCleanupSchedulerTest {

    @InjectMocks
    private PlaylistCleanupScheduler playlistCleanupScheduler;

    @Mock private PlaylistCleanupService playlistCleanupService;

    @Test
    @DisplayName("스케줄러 실행 시 30일 지난 soft delete 데이터 정리 호출")
    void cleanup_success() {
        LocalDateTime before = LocalDateTime.now().minusDays(30);

        // when
        playlistCleanupScheduler.cleanup();

        // then
        ArgumentCaptor<LocalDateTime> thresholdCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(playlistCleanupService, times(1))
                .cleanupDeletedPlaylists(thresholdCaptor.capture());

        LocalDateTime after = LocalDateTime.now().minusDays(30);
        LocalDateTime threshold = thresholdCaptor.getValue();

        assertTrue(!threshold.isBefore(before) && !threshold.isAfter(after),
                "cleanup threshold must be now() - 30 days");
    }
}
