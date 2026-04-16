package com.fivefy.domain.search.service;

import com.fivefy.domain.search.dto.response.SearchAlbumResponse;
import com.fivefy.domain.search.dto.response.SearchArtistResponse;
import com.fivefy.domain.search.dto.response.SearchResponse;
import com.fivefy.domain.search.dto.response.SearchTrackResponse;
import com.fivefy.domain.search.repository.SearchQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchQueryRepository searchQueryRepository;
    private final SearchHistoryService searchHistoryService;

    public SearchResponse search(String keyword, Long userId) {
        String trimmedKeyword = keyword.trim();

        List<SearchArtistResponse> artists = searchQueryRepository.searchArtists(trimmedKeyword)
                .stream().map(SearchArtistResponse::from).toList();

        List<SearchTrackResponse> tracks = searchQueryRepository.searchTracks(trimmedKeyword)
                .stream().map(SearchTrackResponse::from).toList();

        List<SearchAlbumResponse> albums = searchQueryRepository.searchAlbums(trimmedKeyword)
                .stream().map(SearchAlbumResponse::from).toList();

        SearchResponse response = SearchResponse.of(artists, tracks, albums);

        // 로그인 상태면 검색 기록 저장
        if (userId != null) {
            searchHistoryService.saveSearchHistory(userId, trimmedKeyword, response.resultCount());
        }

        return response;
    }
}