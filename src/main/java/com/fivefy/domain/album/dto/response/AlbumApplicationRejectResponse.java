package com.fivefy.domain.album.dto.response;

import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.album.entity.AlbumApplication;

import java.time.LocalDateTime;

/**
 * 앨범 등록 신청 거절 응답 DTO
 */
public record AlbumApplicationRejectResponse(
        Long applicationId,
        ApplicationStatus status,
        Long reviewedByAdminId,
        LocalDateTime reviewedAt,
        String rejectionReason
) {
    public static AlbumApplicationRejectResponse from(AlbumApplication application) {
        return new AlbumApplicationRejectResponse(
                application.getId(),
                application.getStatus(),
                application.getReviewedByAdminId(),
                application.getReviewedAt(),
                application.getRejectionReason()
        );
    }
}