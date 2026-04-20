package com.fivefy.domain.track.dto.response;

import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.track.entity.TrackApplication;

import java.time.LocalDateTime;

/**
 * 트랙 등록 신청 승인 응답 DTO
 */
public record TrackApplicationApproveResponse(
        Long applicationId,
        Long trackId,
        ApplicationStatus status,
        Long reviewedByAdminId,
        LocalDateTime reviewedAt
) {
    public static TrackApplicationApproveResponse from(
            TrackApplication application,
            Long trackId
    ) {
        return new TrackApplicationApproveResponse(
                application.getId(),
                trackId,
                application.getStatus(),
                application.getReviewedByAdminId(),
                application.getReviewedAt()
        );
    }
}