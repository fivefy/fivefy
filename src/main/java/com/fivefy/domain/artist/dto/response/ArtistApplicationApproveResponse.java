package com.fivefy.domain.artist.dto.response;

import com.fivefy.domain.artist.entity.ArtistApplication;

import java.time.LocalDateTime;

/**
 * 아티스트 등록 신청 승인 응답 DTO
 */
public record ArtistApplicationApproveResponse(
        Long applicationId,
        Long artistId,
        String artistType,
        String status,
        Long reviewedByAdminId,
        LocalDateTime reviewedAt
) {
    public static ArtistApplicationApproveResponse from(
            ArtistApplication application,
            Long artistId
    ) {
        return new ArtistApplicationApproveResponse(
                application.getId(),
                artistId,
                application.getArtistType().name(),
                application.getStatus().name(),
                application.getReviewedByAdminId(),
                application.getReviewedAt()
        );
    }
}