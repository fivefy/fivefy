package com.fivefy.domain.artist.dto.response;

import com.fivefy.domain.artist.entity.ArtistApplication;

import java.time.LocalDateTime;

/**
 * 아티스트 등록 요청 거절 응답 DTO
 */
public record ArtistApplicationRejectResponse(
        Long applicationId,
        String artistType,
        String status,
        Long reviewedByAdminId,
        LocalDateTime reviewedAt,
        String rejectionReason
) {
    public static ArtistApplicationRejectResponse from(ArtistApplication application) {
        return new ArtistApplicationRejectResponse(
                application.getId(),
                application.getArtistType().name(),
                application.getStatus().name(),
                application.getReviewedByAdminId(),
                application.getReviewedAt(),
                application.getRejectionReason()
        );
    }
}
