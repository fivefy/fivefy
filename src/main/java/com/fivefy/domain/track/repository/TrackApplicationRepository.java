package com.fivefy.domain.track.repository;

import com.fivefy.domain.track.entity.TrackApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackApplicationRepository extends JpaRepository<TrackApplication, Long>, TrackApplicationQueryRepository {
}