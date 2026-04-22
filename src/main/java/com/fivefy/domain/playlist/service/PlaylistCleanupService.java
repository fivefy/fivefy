package com.fivefy.domain.playlist.service;

import com.fivefy.domain.playlist.repository.PlaylistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistCleanupService {

    private final PlaylistRepository playlistRepository;

    @Transactional
    public int cleanupDeletedPlaylists(LocalDateTime threshold) {
        return playlistRepository.deleteAllSoftDeletedBefore(threshold);
    }
}
