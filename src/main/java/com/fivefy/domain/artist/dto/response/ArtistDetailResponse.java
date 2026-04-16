package com.fivefy.domain.artist.dto.response;

import com.fivefy.domain.artist.entity.Artist;

import java.time.LocalDateTime;

/**
 * 아티스트 상세 조회 응답 DTO
 */
public record ArtistDetailResponse(
        Long artistId,
        String name,
        String artistType,
        String status,
        String bio,
        String profileImageUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ArtistDetailResponse from(Artist artist) {
        return new ArtistDetailResponse(
                artist.getId(),
                artist.getName(),
                artist.getArtistType().name(),
                artist.getStatus().name(),
                artist.getBio(),
                artist.getProfileImageUrl(),
                artist.getCreatedAt(),
                artist.getUpdatedAt()
        );
    }
}