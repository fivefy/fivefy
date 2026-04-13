package com.fivefy.domain.artist.dto.response;

import com.fivefy.domain.artist.entity.ArtistApplication;

import java.time.LocalDateTime;

/**
 * 아티스트 등록 요청 생성 응답 DTO
 */
public record ArtistApplicationCreateResponse(
        Long applicationId,
        String requestedName,
        String status,
        LocalDateTime createdAt
) {
    public static ArtistApplicationCreateResponse from(ArtistApplication application) {
        return new ArtistApplicationCreateResponse(
                application.getId(),
                application.getRequestedName(),
                application.getStatus().name(),
                application.getCreatedAt()
        );
    }
}