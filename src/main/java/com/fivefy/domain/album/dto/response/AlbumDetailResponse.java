package com.fivefy.domain.album.dto.response;

import com.fivefy.domain.album.entity.Album;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 앨범 상세 조회 응답 DTO
 */
public record AlbumDetailResponse(
        Long albumId,
        Long artistId,
        String artistName,
        String title,
        String description,
        String coverImageUrl,
        Long trackCount,
        Long totalDurationSec,
        LocalDateTime publishedAt,
        List<AlbumTrackResponse> tracks
) {
    public static AlbumDetailResponse of(
            Album album,
            String artistName,
            List<AlbumTrackResponse> tracks) {
        return new AlbumDetailResponse(
                album.getId(),
                album.getArtistId(),
                artistName,
                album.getTitle(),
                album.getDescription(),
                album.getCoverImageUrl(),
                album.getTrackCount(),
                album.getTotalDurationSec(),
                album.getPublishedAt(),
                tracks
        );
    }
}
