package com.fivefy.domain.artist.dto.response;

import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.artist.entity.ArtistApplication;
import com.fivefy.domain.artist.enums.ArtistType;

import java.time.LocalDateTime;

/**
 * 아티스트 등록 신청 상세 조회 응답 DTO
 */
public record ArtistApplicationDetailResponse(
        Long applicationId,
        Long requesterUserId,
        String requestedName,
        ArtistType artistType,
        String bio,
        String profileImageUrl,
        ApplicationStatus status,
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
                artistApplication.getArtistType(),
                artistApplication.getBio(),
                artistApplication.getProfileImageUrl(),
                artistApplication.getStatus(),
                artistApplication.getReviewedByAdminId(),
                artistApplication.getReviewedAt(),
                artistApplication.getRejectionReason(),
                artistApplication.getCreatedAt(),
                artistApplication.getUpdatedAt()
        );
    }
}
