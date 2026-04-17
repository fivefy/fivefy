package com.fivefy.domain.search.dto.response;

import com.fivefy.domain.album.entity.Album;

public record SearchAlbumResponse(

        Long id,
        String title,
        String coverImageUrl
) {
    public static SearchAlbumResponse from(Album album) {
        return new SearchAlbumResponse(
                album.getId(),
                album.getTitle(),
                album.getCoverImageUrl()
        );
    }
}