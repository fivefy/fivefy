package com.fivefy.domain.album.dto.response;

import com.fivefy.domain.album.entity.Album;

/**
 * 아티스트별 앨범 목록 응답 DTO
 */
public record ArtistAlbumListResponse(
        Long albumId,
        String title,
        String coverImageUrl,
        Long trackCount
) {
    public static ArtistAlbumListResponse from(Album album) {
        return new ArtistAlbumListResponse(
                album.getId(),
                album.getTitle(),
                album.getCoverImageUrl(),
                album.getTrackCount()
        );
    }
}