package com.fivefy.domain.track.repository;

import com.fivefy.domain.track.entity.TrackApplication;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TrackApplicationRepository extends JpaRepository<TrackApplication, Long>, TrackApplicationQueryRepository {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ta from TrackApplication ta where ta.id = :applicationId")
    Optional<TrackApplication> findByIdForUpdate(@Param("applicationId") Long applicationId);
}