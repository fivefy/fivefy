package com.fivefy.domain.artist.dto.response;

import java.time.LocalDateTime;

/**
 * 아티스트 상세 조회 응답 DTO
 */
public record ArtistDetailResponse(
        Long artistId,
        String name,
        String artistType,
        String bio,
        String profileImageUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}