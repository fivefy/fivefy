package com.fivefy.domain.playback.repository;

import com.fivefy.domain.playback.entity.Playback;
import com.fivefy.domain.playback.enums.PlaybackStatus;
import com.fivefy.domain.popularchart.dto.projection.TrackPlayCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PlaybackRepository extends JpaRepository<Playback, Long> {

    Optional<Playback> findTopByUserIdAndPlaylistIdAndTrackIdAndSessionIdOrderByIdDesc(
            Long userId, Long playlistId, Long trackId, String sessionId
    );

    Optional<Playback> findTopByUserIdAndSessionIdAndStatusOrderByIdDesc(
            Long userId, String sessionId, PlaybackStatus status
    );

    Optional<Playback> findByIdAndUserId(Long playbackId, Long userId);

    List<Playback> findAllByUserIdOrderByIdDesc(Long userId);

    // 주어진 기간(startDate ~ endDate) 동안 track별 재생 수 집계
    @Query(value = """
        SELECT t.track_id AS trackId, COUNT(*) AS playCount
        FROM (
            SELECT DISTINCT p.session_id, p.track_id
            FROM playbacks p
            WHERE p.ended_at >= :startDate
              AND p.ended_at < :endDate
              AND p.played_duration >= :minimumPlayedDuration
              AND p.status IN ('STOPPED', 'SKIPPED', 'COMPLETED')
        ) t
        GROUP BY t.track_id
        ORDER BY COUNT(*) DESC, t.track_id ASC
        """, nativeQuery = true)
    List<TrackPlayCountProjection> countWeeklyValidPlayByTrack(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("minimumPlayedDuration") int minimumPlayedDuration
    );
}
