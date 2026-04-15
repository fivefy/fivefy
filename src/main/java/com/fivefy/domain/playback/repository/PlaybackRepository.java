package com.fivefy.domain.playback.repository;

import com.fivefy.domain.playback.entity.Playback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaybackRepository extends JpaRepository<Playback, Long> {

    Optional<Playback> findTopByUserIdAndPlaylistIdAndTrackIdAndSessionIdOrderByIdDesc(Long userId, Long playlistId, Long trackId, String sessionId);
    List<Playback> findAllByUserIdOrderByIdDesc(Long userId);
}
