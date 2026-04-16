package com.fivefy.domain.playback.repository;

import com.fivefy.domain.playback.dto.projection.TrackPlayCountDto;
import com.fivefy.domain.playback.entity.Playback;
import com.fivefy.domain.playback.enums.PlaybackStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PlaybackRepository extends JpaRepository<Playback, Long> {

    Optional<Playback> findTopByUserIdAndPlaylistIdAndTrackIdAndSessionIdOrderByIdDesc(Long userId, Long playlistId, Long trackId, String sessionId);
    Optional<Playback> findTopByUserIdAndSessionIdAndStatusOrderByIdDesc(Long userId, String sessionId, PlaybackStatus status);
    Optional<Playback> findByIdAndUserId(Long playbackId, Long userId);
    List<Playback> findAllByUserIdOrderByIdDesc(Long userId);
    @Query("""
    select new com.fivefy.domain.playback.dto.projection.TrackPlayCountDto(
        p.trackId,
        count(p)
    )
    from Playback p
    where p.endedAt >= :startDate
      and p.endedAt < :endDate
    group by p.trackId
    order by count(p) desc
""")
    List<TrackPlayCountDto> countWeeklyPlayByTrack(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
