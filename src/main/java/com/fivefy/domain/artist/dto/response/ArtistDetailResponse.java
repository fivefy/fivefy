package com.fivefy.domain.artist.dto.response;

import com.fivefy.domain.artist.entity.Artist;
import com.fivefy.domain.artist.enums.ArtistStatus;
import com.fivefy.domain.artist.enums.ArtistType;

import java.time.LocalDateTime;

/**
 * 아티스트 상세 조회 응답 DTO
 */
public record ArtistDetailResponse(
        Long artistId,
        String name,
        ArtistType artistType,
        ArtistStatus status,
        String bio,
        String profileImageUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ArtistDetailResponse from(Artist artist) {
        return new ArtistDetailResponse(
                artist.getId(),
                artist.getName(),
                artist.getArtistType(),
                artist.getStatus(),
                artist.getBio(),
                artist.getProfileImageUrl(),
                artist.getCreatedAt(),
                artist.getUpdatedAt()
        );
    }
}