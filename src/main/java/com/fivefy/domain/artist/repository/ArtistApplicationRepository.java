package com.fivefy.domain.artist.repository;

import com.fivefy.domain.artist.entity.ArtistApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtistApplicationRepository extends JpaRepository<ArtistApplication, Long> {
    List<ArtistApplication> findAllByRequesterUserIdOrderByCreatedAtDesc(Long requesterUserId);
}