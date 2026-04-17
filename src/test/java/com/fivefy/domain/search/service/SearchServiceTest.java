package com.fivefy.domain.search.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.search.dto.response.SearchAlbumResponse;
import com.fivefy.domain.search.dto.response.SearchArtistResponse;
import com.fivefy.domain.search.dto.response.SearchResponse;
import com.fivefy.domain.search.dto.response.SearchTrackResponse;
import com.fivefy.domain.search.enums.SearchErrorCode;
import com.fivefy.domain.search.repository.SearchQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @InjectMocks
    private SearchService searchService;

    @Mock
    private SearchQueryRepository searchQueryRepository;

    @Mock
    private SearchHistoryService searchHistoryService;

    private final Pageable pageable = PageRequest.of(0, 20);

    @Test
    @DisplayName("빈 검색어 입력 시 예외가 발생한다")
    void search_blankKeyword_throwsException() {
        assertThatThrownBy(() -> searchService.search("   ", pageable, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(SearchErrorCode.ERR_SEARCH_KEYWORD_BLANK.getMessage());
    }

    @Test
    @DisplayName("정상 검색 시 SearchResponse를 반환한다")
    void search_validKeyword_returnsSearchResponse() {
        // given
        given(searchQueryRepository.searchArtists(anyString(), anyInt()))
                .willReturn(List.of());
        given(searchQueryRepository.searchTracks(anyString(), anyList(), any(Pageable.class)))
                .willReturn(Page.empty());
        given(searchQueryRepository.searchAlbums(anyString(), anyList(), any(Pageable.class)))
                .willReturn(Page.empty());

        // when
        SearchResponse response = searchService.search("루나", pageable, 1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.artists()).isNotNull();
        assertThat(response.tracks()).isNotNull();
        assertThat(response.albums()).isNotNull();
    }

    @Test
    @DisplayName("로그인 상태에서 검색 시 검색 기록 저장이 호출된다")
    void search_loggedIn_savesSearchHistory() {
        // given
        Long userId = 1L;
        given(searchQueryRepository.searchArtists(anyString(), anyInt()))
                .willReturn(List.of());
        given(searchQueryRepository.searchTracks(anyString(), anyList(), any(Pageable.class)))
                .willReturn(Page.empty());
        given(searchQueryRepository.searchAlbums(anyString(), anyList(), any(Pageable.class)))
                .willReturn(Page.empty());

        // when
        searchService.search("루나", pageable, userId);

        // then
        verify(searchHistoryService, times(1))
                .saveSearchHistory(eq(userId), eq("루나"), anyInt());
    }

    @Test
    @DisplayName("비로그인 상태에서 검색 시 userId null로 검색 기록 저장이 호출된다")
    void search_notLoggedIn_savesSearchHistoryWithNullUserId() {
        // given
        given(searchQueryRepository.searchArtists(anyString(), anyInt()))
                .willReturn(List.of());
        given(searchQueryRepository.searchTracks(anyString(), anyList(), any(Pageable.class)))
                .willReturn(Page.empty());
        given(searchQueryRepository.searchAlbums(anyString(), anyList(), any(Pageable.class)))
                .willReturn(Page.empty());

        // when
        searchService.search("루나", pageable, null);

        // then
        verify(searchHistoryService, times(1))
                .saveSearchHistory(eq(null), eq("루나"), anyInt());
    }

    @Test
    @DisplayName("검색 기록 저장 실패 시에도 검색 결과가 정상 반환된다")
    void search_saveHistoryFails_stillReturnsResponse() {
        // given
        given(searchQueryRepository.searchArtists(anyString(), anyInt()))
                .willReturn(List.of());
        given(searchQueryRepository.searchTracks(anyString(), anyList(), any(Pageable.class)))
                .willReturn(Page.empty());
        given(searchQueryRepository.searchAlbums(anyString(), anyList(), any(Pageable.class)))
                .willReturn(Page.empty());
        doThrow(new RuntimeException("Redis 장애"))
                .when(searchHistoryService).saveSearchHistory(any(), anyString(), anyInt());

        // when
        SearchResponse response = searchService.search("루나", pageable, 1L);

        // then
        assertThat(response).isNotNull();
    }
}