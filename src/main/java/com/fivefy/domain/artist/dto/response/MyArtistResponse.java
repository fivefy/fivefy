package com.fivefy.domain.artist.dto.response;

import com.fivefy.domain.artist.entity.Artist;

import java.time.LocalDateTime;

/**
 * 내 아티스트 목록 조회 응답 DTO
 */
public record MyArtistResponse(
        Long artistId,
        String name,
        String bio,
        String profileImageUrl,
        String artistType,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MyArtistResponse from(Artist artist) {
        return new MyArtistResponse(
                artist.getId(),
                artist.getName(),
                artist.getBio(),
                artist.getProfileImageUrl(),
                artist.getArtistType().name(),
                artist.getCreatedAt(),
                artist.getUpdatedAt()
        );
    }
}