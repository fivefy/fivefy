package com.fivefy.domain.playlisttrack.repository;

import com.fivefy.domain.playlisttrack.entity.PlaylistTrack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaylistTrackRepository extends JpaRepository<PlaylistTrack, Long> {

    List<PlaylistTrack> findAllByPlaylistIdOrderByPositionAsc(Long playlistId);
    Optional<PlaylistTrack> findByPlaylistIdAndTrackId(Long playlistId, Long trackId);
    boolean existsByPlaylistIdAndTrackId(Long playlistId, Long trackId);
    int countByPlaylistId(Long playlistId);
}
