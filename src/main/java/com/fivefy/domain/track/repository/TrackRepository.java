package com.fivefy.domain.track.repository;

import com.fivefy.domain.track.entity.Track;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackRepository extends JpaRepository<Track, Long>, TrackQueryRepository {
}