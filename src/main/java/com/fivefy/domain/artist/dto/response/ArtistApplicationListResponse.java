package com.fivefy.domain.artist.dto.response;

import com.fivefy.domain.artist.entity.ArtistApplication;

import java.time.LocalDateTime;

/**
 * 아티스트 등록 신청 목록 조회 응답 DTO
 */
public record ArtistApplicationListResponse(
        Long applicationId,
        Long requesterUserId,
        String requestedName,
        String artistType,
        String status,
        LocalDateTime createdAt
) {
    public static ArtistApplicationListResponse from(ArtistApplication artistApplication) {
        return new ArtistApplicationListResponse(
                artistApplication.getId(),
                artistApplication.getRequesterUserId(),
                artistApplication.getRequestedName(),
                artistApplication.getArtistType().name(),
                artistApplication.getStatus().name(),
                artistApplication.getCreatedAt()
        );
    }
}
