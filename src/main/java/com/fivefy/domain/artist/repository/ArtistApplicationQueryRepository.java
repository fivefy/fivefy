package com.fivefy.domain.artist.repository;

import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.artist.entity.ArtistApplication;
import com.fivefy.domain.artist.enums.ArtistType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * ArtistApplication Querydsl 전용 Repository
 */
public interface ArtistApplicationQueryRepository {

    boolean existsActiveApplication(Long requesterUserId, String requestedName, ArtistType artistType);

    List<ArtistApplication> searchMyArtistApplications(Long requesterUserId);

    Page<ArtistApplication> searchArtistApplications(ApplicationStatus status, Pageable pageable);
}