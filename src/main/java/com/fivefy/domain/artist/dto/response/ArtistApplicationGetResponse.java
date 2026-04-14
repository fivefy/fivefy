package com.fivefy.domain.artist.dto.response;

import com.fivefy.domain.artist.entity.ArtistApplication;

import java.time.LocalDateTime;

/**
 * 아티스트 등록 요청 조회 응답 DTO
 */
public record ArtistApplicationGetResponse(
        Long applicationId,
        String requestedName,
        String status,
        LocalDateTime createdAt
) {

    public static ArtistApplicationGetResponse from(ArtistApplication artistApplication) {
        return new ArtistApplicationGetResponse(
                artistApplication.getId(),
                artistApplication.getRequestedName(),
                artistApplication.getStatus().name(),
                artistApplication.getCreatedAt()
        );
    }
}