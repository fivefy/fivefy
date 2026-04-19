package com.fivefy.domain.track.repository;

import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.track.entity.TrackApplication;
import com.fivefy.domain.track.enums.TrackType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TrackApplicationQueryRepository {
    boolean existsActiveApplication(
            Long requesterUserId,
            TrackType trackType,
            Long artistId,
            Long albumId,
            String title
    );

    List<TrackApplication> searchMyTrackApplications(Long requesterUserId);

    Page<TrackApplication> searchTrackApplications(
            ApplicationStatus status,
            Pageable pageable
    );
}
