package com.fivefy.domain.search.dto.cache;

import com.fivefy.domain.search.dto.response.SearchAlbumResponse;
import com.fivefy.domain.search.dto.response.SearchArtistResponse;
import com.fivefy.domain.search.dto.response.SearchResponse;
import com.fivefy.domain.search.dto.response.SearchTrackResponse;

import java.util.List;

public record SearchCacheDto(
        List<SearchArtistResponse> artists,
        List<SearchTrackResponse> tracks,
        List<SearchAlbumResponse> albums,
        boolean tracksHasNext,
        boolean albumsHasNext,
        long tracksTotalElements,
        long albumsTotalElements,
        Integer resultCount
) {
    public static SearchCacheDto from(SearchResponse response) {
        return new SearchCacheDto(
                response.artists(),
                response.tracks().getContent(),
                response.albums().getContent(),
                response.tracks().hasNext(),
                response.albums().hasNext(),
                response.tracks().getTotalElements(),
                response.albums().getTotalElements(),
                response.resultCount()
        );
    }
}