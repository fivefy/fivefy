package com.fivefy.domain.artist.dto.response;

import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.artist.entity.ArtistApplication;
import com.fivefy.domain.artist.enums.ArtistType;

import java.time.LocalDateTime;

/**
 * 아티스트 등록 신청 승인 응답 DTO
 */
public record ArtistApplicationApproveResponse(
        Long applicationId,
        Long artistId,
        ArtistType artistType,
        ApplicationStatus status,
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
                application.getArtistType(),
                application.getStatus(),
                application.getReviewedByAdminId(),
                application.getReviewedAt()
        );
    }
}