package com.fivefy.domain.artist.dto.response;

import com.fivefy.domain.artist.entity.ArtistApplication;

import java.time.LocalDateTime;

/**
 * 아티스트 등록 신청 생성 및 내 아티스트 등록 신청 목록 조회 응답 DTO
 */
public record ArtistApplicationResponse(
        Long applicationId,
        String requestedName,
        String artistType,
        String status,
        LocalDateTime createdAt
) {
    public static ArtistApplicationResponse from(ArtistApplication artistApplication) {
        return new ArtistApplicationResponse(
                artistApplication.getId(),
                artistApplication.getRequestedName(),
                artistApplication.getArtistType().name(),
                artistApplication.getStatus().name(),
                artistApplication.getCreatedAt()
        );
    }
}