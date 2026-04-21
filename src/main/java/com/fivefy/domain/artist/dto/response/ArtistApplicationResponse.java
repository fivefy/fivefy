package com.fivefy.domain.artist.dto.response;

import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.artist.entity.ArtistApplication;
import com.fivefy.domain.artist.enums.ArtistType;

import java.time.LocalDateTime;

/**
 * 아티스트 등록 신청 생성 및 내 아티스트 등록 신청 목록 조회 응답 DTO
 */
public record ArtistApplicationResponse(
        Long applicationId,
        String requestedName,
        ArtistType artistType,
        ApplicationStatus status,
        LocalDateTime createdAt
) {
    public static ArtistApplicationResponse from(ArtistApplication artistApplication) {
        return new ArtistApplicationResponse(
                artistApplication.getId(),
                artistApplication.getRequestedName(),
                artistApplication.getArtistType(),
                artistApplication.getStatus(),
                artistApplication.getCreatedAt()
        );
    }
}