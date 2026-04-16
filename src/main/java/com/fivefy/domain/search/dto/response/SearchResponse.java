package com.fivefy.domain.search.dto.response;

import java.util.List;

public record SearchResponse(

        List<SearchArtistResponse> artists,
        List<SearchTrackResponse> tracks,
        List<SearchAlbumResponse> albums,
        Integer resultCount
) {
    public static SearchResponse of(
            List<SearchArtistResponse> artists,
            List<SearchTrackResponse> tracks,
            List<SearchAlbumResponse> albums
    ) {
        Integer resultCount = artists.size() + tracks.size() + albums.size();
        return new SearchResponse(artists, tracks, albums, resultCount);
    }
}