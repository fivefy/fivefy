package com.fivefy.domain.playback.repository;

import com.fivefy.domain.playback.entity.Playback;
import com.fivefy.domain.playback.enums.PlaybackStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaybackRepository extends JpaRepository<Playback, Long> {

    Optional<Playback> findTopByUserIdAndPlaylistIdAndTrackIdAndSessionIdOrderByIdDesc(Long userId, Long playlistId, Long trackId, String sessionId);
    Optional<Playback> findTopByUserIdAndSessionIdAndStatusOrderByIdDesc(Long userId, String sessionId, PlaybackStatus status);
    Optional<Playback> findByIdAndUserId(Long playbackId, Long userId);
    List<Playback> findAllByUserIdOrderByIdDesc(Long userId);
}
