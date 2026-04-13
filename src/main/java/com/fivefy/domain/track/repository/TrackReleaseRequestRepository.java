package com.fivefy.domain.track.repository;

import com.fivefy.domain.track.entity.TrackReleaseRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackReleaseRequestRepository extends JpaRepository<TrackReleaseRequest, Long> {
}