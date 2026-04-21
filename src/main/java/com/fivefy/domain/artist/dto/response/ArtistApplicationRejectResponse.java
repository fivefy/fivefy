package com.fivefy.domain.artist.dto.response;

import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.artist.entity.ArtistApplication;
import com.fivefy.domain.artist.enums.ArtistType;

import java.time.LocalDateTime;

/**
 * 아티스트 등록 신청 거절 응답 DTO
 */
public record ArtistApplicationRejectResponse(
        Long applicationId,
        ArtistType artistType,
        ApplicationStatus status,
        Long reviewedByAdminId,
        LocalDateTime reviewedAt,
        String rejectionReason
) {
    public static ArtistApplicationRejectResponse from(ArtistApplication application) {
        return new ArtistApplicationRejectResponse(
                application.getId(),
                application.getArtistType(),
                application.getStatus(),
                application.getReviewedByAdminId(),
                application.getReviewedAt(),
                application.getRejectionReason()
        );
    }
}
