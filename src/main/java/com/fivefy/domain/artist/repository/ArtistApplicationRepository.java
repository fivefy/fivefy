package com.fivefy.domain.artist.repository;

import com.fivefy.domain.artist.entity.ArtistApplication;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ArtistApplicationRepository
        extends JpaRepository<ArtistApplication, Long>, ArtistApplicationQueryRepository {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select aa from ArtistApplication aa where aa.id = :applicationId")
    Optional<ArtistApplication> findByIdForUpdate(@Param("applicationId") Long applicationId);
}