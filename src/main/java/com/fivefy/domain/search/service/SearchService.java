package com.fivefy.domain.search.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.search.dto.response.SearchAlbumResponse;
import com.fivefy.domain.search.dto.response.SearchArtistResponse;
import com.fivefy.domain.search.dto.response.SearchResponse;
import com.fivefy.domain.search.dto.response.SearchTrackResponse;
import com.fivefy.domain.search.enums.SearchErrorCode;
import com.fivefy.domain.search.repository.SearchQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int ARTIST_LIMIT = 5;

    private final SearchQueryRepository searchQueryRepository;
    private final SearchHistoryService searchHistoryService;

    @Transactional(readOnly = true)
    public SearchResponse search(String keyword, Pageable pageable, Long userId) {
        String trimmedKeyword = keyword.trim();

        if (trimmedKeyword.isBlank()) {
            throw new BusinessException(SearchErrorCode.ERR_SEARCH_KEYWORD_BLANK);
        }

        List<SearchArtistResponse> artists = searchQueryRepository
                .searchArtists(trimmedKeyword, ARTIST_LIMIT)
                .stream().map(SearchArtistResponse::from).toList();

        List<Long> artistIds = artists.stream()
                .map(SearchArtistResponse::id)
                .toList();

        Page<SearchTrackResponse> tracks = searchQueryRepository
                .searchTracks(trimmedKeyword, artistIds, pageable)
                .map(SearchTrackResponse::from);

        Page<SearchAlbumResponse> albums = searchQueryRepository
                .searchAlbums(trimmedKeyword, artistIds, pageable)
                .map(SearchAlbumResponse::from);
        SearchResponse response = SearchResponse.of(artists, tracks, albums);

        try {
            searchHistoryService.saveSearchHistory(userId, trimmedKeyword, response.resultCount());
        } catch (Exception e) {
            log.warn("검색 기록 저장 실패: {}", e.getMessage());
        }

        return response;
    }
}
