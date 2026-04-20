package com.fivefy.domain.track.repository;

import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.track.entity.TrackApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
/**
 * TrackApplication Querydsl 전용 Repository
 */
public interface TrackApplicationQueryRepository {
    boolean existsPendingFreeCreationApplication(
            Long requesterUserId,
            String title,
            String audioUrl
    );

    boolean existsPendingOfficialReleaseApplication(
            Long requesterUserId,
            Long artistId,
            Long albumId,
            Long trackNumber,
            String title
    );

    List<TrackApplication> searchMyTrackApplications(Long requesterUserId);

    Page<TrackApplication> searchTrackApplications(
            ApplicationStatus status,
            Pageable pageable
    );
}
