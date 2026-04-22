package com.fivefy.domain.playlist.scheduler;

import com.fivefy.domain.playlist.service.PlaylistCleanupService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class PlaylistCleanupScheduler {

    private final PlaylistCleanupService playlistCleanupService;

    @Scheduled(cron = "0 0 3 * * *")
    @SchedulerLock(name = "playlistCleanupScheduler_cleanup", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    public void cleanup() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        playlistCleanupService.cleanupDeletedPlaylists(threshold);
    }
}
