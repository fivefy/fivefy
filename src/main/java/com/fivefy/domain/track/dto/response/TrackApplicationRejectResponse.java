package com.fivefy.domain.track.dto.response;

import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.track.entity.TrackApplication;

import java.time.LocalDateTime;

/**
 * 트랙 등록 신청 거절 응답 DTO
 */
public record TrackApplicationRejectResponse(
        Long applicationId,
        ApplicationStatus status,
        Long reviewedByAdminId,
        LocalDateTime reviewedAt,
        String rejectionReason
) {
    public static TrackApplicationRejectResponse from(TrackApplication application) {
        return new TrackApplicationRejectResponse(
                application.getId(),
                application.getStatus(),
                application.getReviewedByAdminId(),
                application.getReviewedAt(),
                application.getRejectionReason()
        );
    }
}