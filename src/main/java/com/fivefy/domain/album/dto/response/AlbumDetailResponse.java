package com.fivefy.domain.album.dto.response;

import com.fivefy.domain.album.entity.Album;

import java.time.LocalDateTime;

public record AlbumDetailResponse(
        Long albumId,
        Long artistId,
        String artistName,
        String title,
        String description,
        String coverImageUrl,
        Long trackCount,
        Long totalDurationSec,
        LocalDateTime publishedAt
) {
    public static AlbumDetailResponse of(Album album, String artistName) {
        return new AlbumDetailResponse(
                album.getId(),
                album.getArtistId(),
                artistName,
                album.getTitle(),
                album.getDescription(),
                album.getCoverImageUrl(),
                album.getTrackCount(),
                album.getTotalDurationSec(),
                album.getPublishedAt()
        );
    }
}
