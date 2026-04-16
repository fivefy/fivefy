package com.fivefy.domain.search.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record SearchResponse(
        List<SearchArtistResponse> artists,
        Page<SearchTrackResponse> tracks,
        Page<SearchAlbumResponse> albums,
        Integer resultCount
) {
    public static SearchResponse of(
            List<SearchArtistResponse> artists,
            Page<SearchTrackResponse> tracks,
            Page<SearchAlbumResponse> albums
    ) {
        Integer resultCount = artists.size() + (int) tracks.getTotalElements() + (int) albums.getTotalElements();
        return new SearchResponse(artists, tracks, albums, resultCount);
    }
}