package com.fivefy.domain.search.dto.response;

import com.fivefy.domain.artist.entity.Artist;

public record SearchArtistResponse(

        Long id,
        String name,
        String profileImageUrl
) {
    public static SearchArtistResponse from(Artist artist) {
        return new SearchArtistResponse(
                artist.getId(),
                artist.getName(),
                artist.getProfileImageUrl()
        );
    }
}