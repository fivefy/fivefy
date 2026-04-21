package com.fivefy.domain.artist.dto.response;

import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.artist.entity.ArtistApplication;
import com.fivefy.domain.artist.enums.ArtistType;

import java.time.LocalDateTime;

/**
 * 아티스트 등록 신청 목록 조회 응답 DTO
 */
public record ArtistApplicationListResponse(
        Long applicationId,
        Long requesterUserId,
        String requestedName,
        ArtistType artistType,
        ApplicationStatus status,
        LocalDateTime createdAt
) {
    public static ArtistApplicationListResponse from(ArtistApplication artistApplication) {
        return new ArtistApplicationListResponse(
                artistApplication.getId(),
                artistApplication.getRequesterUserId(),
                artistApplication.getRequestedName(),
                artistApplication.getArtistType(),
                artistApplication.getStatus(),
                artistApplication.getCreatedAt()
        );
    }
}
