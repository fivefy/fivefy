package com.fivefy.domain.track.dto.response;

import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.track.entity.TrackApplication;
import com.fivefy.domain.track.enums.TrackType;

import java.time.LocalDateTime;

/**
 * 트랙 등록 신청 목록 응답 DTO
 */
public record TrackApplicationListResponse(
        Long applicationId,
        Long requesterUserId,
        TrackType trackType,
        String title,
        ApplicationStatus status,
        LocalDateTime createdAt
) {
    public static TrackApplicationListResponse from(TrackApplication application) {
        return new TrackApplicationListResponse(
                application.getId(),
                application.getRequesterUserId(),
                application.getTrackType(),
                application.getTitle(),
                application.getStatus(),
                application.getCreatedAt()
        );
    }
}