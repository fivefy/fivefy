package com.fivefy.domain.artist.dto.response;

import com.fivefy.domain.artist.entity.ArtistApplication;

import java.time.LocalDateTime;

/**
 * 아티스트 등록 요청 상세 조회 응답 DTO
 */
public record ArtistApplicationDetailResponse(
        Long applicationId,
        Long requesterUserId,
        String requestedName,
        String bio,
        String profileImageUrl,
        String artistType,
        String status,
        Long reviewedByAdminId,
        LocalDateTime reviewedAt,
        String rejectionReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ArtistApplicationDetailResponse from(ArtistApplication artistApplication) {
        return new ArtistApplicationDetailResponse(
                artistApplication.getId(),
                artistApplication.getRequesterUserId(),
                artistApplication.getRequestedName(),
                artistApplication.getBio(),
                artistApplication.getProfileImageUrl(),
                artistApplication.getArtistType().name(),
                artistApplication.getStatus().name(),
                artistApplication.getReviewedByAdminId(),
                artistApplication.getReviewedAt(),
                artistApplication.getRejectionReason(),
                artistApplication.getCreatedAt(),
                artistApplication.getUpdatedAt()
        );
    }
}
