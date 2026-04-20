package com.fivefy.domain.search.controller;

import com.fivefy.common.config.security.JwtUtil;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.search.dto.response.SearchArtistResponse;
import com.fivefy.domain.search.dto.response.SearchResponse;
import com.fivefy.domain.search.enums.SearchErrorCode;
import com.fivefy.domain.search.service.SearchHistoryService;
import com.fivefy.domain.search.service.SearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = {SearchController.class, SearchHistoryController.class})
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchService searchService;

    @MockitoBean
    private SearchHistoryService searchHistoryService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;;

    @Nested
    @DisplayName("검색")
    class Search {

        @Test
        @DisplayName("정상 검색 시 200을 반환한다")
        void search_validKeyword_returns200() throws Exception {
            // given
            SearchResponse response = new SearchResponse(List.of(), Page.empty(), Page.empty(), 0);
            given(searchService.search(anyString(), any(), any())).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/search")
                            .param("keyword", "루나"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("검색 성공"));
        }

        @Test
        @DisplayName("검색 결과에 artists, tracks, albums, resultCount가 포함된다")
        void search_returnsResultData() throws Exception {
            // given
            SearchArtistResponse artist = new SearchArtistResponse(1L, "루나", "https://img.example.com/luna.jpg");
            PageImpl<Object> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
            SearchResponse response = new SearchResponse(List.of(artist), Page.empty(), Page.empty(), 1);
            given(searchService.search(anyString(), any(), any())).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/search")
                            .param("keyword", "루나"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.artists[0].id").value(1L))
                    .andExpect(jsonPath("$.data.artists[0].name").value("루나"))
                    .andExpect(jsonPath("$.data.resultCount").value(1));
        }

        @Test
        @DisplayName("공백 keyword로 요청 시 서비스에서 예외가 발생하여 400을 반환한다")
        void search_blankKeyword_returns400() throws Exception {
            // given
            given(searchService.search(anyString(), any(), any()))
                    .willThrow(new BusinessException(SearchErrorCode.ERR_SEARCH_KEYWORD_BLANK));

            // when & then
            mockMvc.perform(get("/api/search")
                            .param("keyword", "   "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(SearchErrorCode.ERR_SEARCH_KEYWORD_BLANK.getMessage()));
        }
    }

    @Nested
    @DisplayName("최근 검색 기록 조회")
    class GetRecentSearchHistories {

        @Test
        @DisplayName("최근 검색 기록 조회 시 200을 반환한다")
        void getRecentSearchHistories_returns200() throws Exception {
            // given
            given(searchHistoryService.getRecentSearchHistories(any()))
                    .willReturn(List.of("아이유", "루나"));

            // when & then
            mockMvc.perform(get("/api/search-histories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("최근 검색 기록 조회 성공"))
                    .andExpect(jsonPath("$.data[0]").value("아이유"))
                    .andExpect(jsonPath("$.data[1]").value("루나"));
        }

        @Test
        @DisplayName("최근 검색 기록이 없으면 빈 리스트를 반환한다")
        void getRecentSearchHistories_empty_returnsEmptyList() throws Exception {
            // given
            given(searchHistoryService.getRecentSearchHistories(any()))
                    .willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/search-histories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    @Nested
    @DisplayName("검색 기록 삭제")
    class DeleteAllRecentSearchHistories {

        @Test
        @DisplayName("전체 검색 기록 삭제 시 200을 반환한다")
        void deleteAllRecentSearchHistories_returns200() throws Exception {
            // given
            doNothing().when(searchHistoryService).deleteAllRecentSearchHistories(any());

            // when & then
            mockMvc.perform(delete("/api/search-histories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("최근 검색 기록 전체 삭제 성공"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/search-histories/recent")
    class DeleteRecentSearchHistory {

        @Test
        @DisplayName("개별 검색 기록 삭제 시 200을 반환한다")
        void deleteRecentSearchHistory_returns200() throws Exception {
            // given
            doNothing().when(searchHistoryService).deleteRecentSearchHistory(any(), anyString());

            // when & then
            mockMvc.perform(delete("/api/search-histories/recent")
                            .param("keyword", "아이유"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("최근 검색 기록 삭제 성공"));
        }
    }
}