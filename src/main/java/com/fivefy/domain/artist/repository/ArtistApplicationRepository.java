package com.fivefy.domain.artist.repository;

import com.fivefy.domain.artist.entity.ArtistApplication;
import com.fivefy.domain.artist.enums.ArtistType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ArtistApplicationRepository extends JpaRepository<ArtistApplication, Long>, ArtistApplicationQueryRepository {

    List<ArtistApplication> findAllByRequesterUserIdOrderByCreatedAtDesc(Long requesterUserId);

    @Query("""
        select count(a) > 0
        from ArtistApplication a
        where a.requesterUserId = :requesterUserId
          and a.requestedName = :requestedName
          and a.artistType = :artistType
          and a.status in (
                  com.fivefy.common.enums.ApplicationStatus.PENDING,
                  com.fivefy.common.enums.ApplicationStatus.APPROVED)
        """)
    boolean existsActiveApplication(Long requesterUserId, String requestedName, ArtistType artistType);
}