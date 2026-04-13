package com.fivefy.domain.artist.repository;

import com.fivefy.domain.artist.entity.ArtistApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistApplicationRepository extends JpaRepository<ArtistApplication, Long> {
}