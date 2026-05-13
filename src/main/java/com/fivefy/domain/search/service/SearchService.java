package com.fivefy.domain.search.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.search.dto.cache.SearchCacheDto;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;
import java.time.Duration;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int ARTIST_LIMIT = 5;
    private static final Duration SEARCH_CACHE_TTL = Duration.ofMinutes(5);

    private final SearchQueryRepository searchQueryRepository;
    private final SearchHistoryService searchHistoryService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;


    @Transactional(readOnly = true)
    public SearchResponse search(String keyword, Pageable pageable, Long userId) {
        String trimmedKeyword = keyword.trim();

        if (trimmedKeyword.isBlank()) {
            throw new BusinessException(SearchErrorCode.ERR_SEARCH_KEYWORD_BLANK);
        }

        // 캐시 조회
        String cacheKey = "search:" + trimmedKeyword
                + ":page:" + pageable.getPageNumber()
                + ":" + pageable.getPageSize()
                + ":" + pageable.getSort();
        SearchResponse cached = getFromCache(cacheKey, pageable);
        if (cached != null) return cached;

        // DB 조회
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

        // 캐시 저장
        saveToCache(cacheKey, response);

        try {
            searchHistoryService.saveSearchHistory(userId, trimmedKeyword, response.resultCount());
        } catch (Exception e) {
            log.warn("검색 기록 저장 실패: {}", e.getMessage());
        }

        return response;
    }

    private SearchResponse getFromCache(String cacheKey, Pageable pageable) {
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached == null) return null;

            SearchCacheDto dto = objectMapper.readValue(cached, SearchCacheDto.class);

            Page<SearchTrackResponse> tracks = new PageImpl<>(
                    dto.tracks(), pageable, dto.tracksTotalElements());
            Page<SearchAlbumResponse> albums = new PageImpl<>(
                    dto.albums(), pageable, dto.albumsTotalElements());

            return new SearchResponse(dto.artists(), tracks, albums, dto.resultCount());
        } catch (Exception e) {
            log.warn("검색 캐시 조회 실패 — DB 조회로 폴백: {}", e.getMessage());
            return null;
        }
    }

    private void saveToCache(String cacheKey, SearchResponse response) {
        try {
            SearchCacheDto dto = SearchCacheDto.from(response);
            stringRedisTemplate.opsForValue().set(
                    cacheKey, objectMapper.writeValueAsString(dto), SEARCH_CACHE_TTL);
        } catch (Exception e) {
            log.warn("검색 캐시 저장 실패: {}", e.getMessage());
        }
    }
}
