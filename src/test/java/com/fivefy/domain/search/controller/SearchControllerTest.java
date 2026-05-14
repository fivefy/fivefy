package com.fivefy.domain.search.controller;

import com.fivefy.common.config.security.JwtUtil;
import com.fivefy.common.docs.RestDocsSupport;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.search.dto.response.SearchAlbumResponse;
import com.fivefy.domain.search.dto.response.SearchArtistResponse;
import com.fivefy.domain.search.dto.response.SearchResponse;
import com.fivefy.domain.search.dto.response.SearchTrackResponse;
import com.fivefy.domain.search.enums.SearchErrorCode;
import com.fivefy.domain.search.service.SearchHistoryService;
import com.fivefy.domain.search.service.SearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {SearchController.class, SearchHistoryController.class})
class SearchControllerTest extends RestDocsSupport {

    @MockitoBean
    private SearchService searchService;

    @MockitoBean
    private SearchHistoryService searchHistoryService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @Nested
    @WithMockUser
    @DisplayName("통합 검색 API")
    class Search {

        @Test
        @DisplayName("정상 검색 시 200 반환")
        void search_success() throws Exception {
            SearchArtistResponse artist = new SearchArtistResponse(
                    1L, "루나", "https://img.example.com/luna.jpg"
            );
            SearchTrackResponse track = new SearchTrackResponse(
                    10L, "Moonlight", "POP", 210L
            );
            SearchAlbumResponse album = new SearchAlbumResponse(
                    100L, "Phase", "https://img.example.com/phase.jpg"
            );

            PageImpl<SearchTrackResponse> tracks = new PageImpl<>(
                    List.of(track), PageRequest.of(0, 20), 1
            );
            PageImpl<SearchAlbumResponse> albums = new PageImpl<>(
                    List.of(album), PageRequest.of(0, 20), 1
            );

            SearchResponse response = new SearchResponse(
                    List.of(artist), tracks, albums, 3
            );

            given(searchService.search(anyString(), any(), any())).willReturn(response);

            mockMvc.perform(get("/api/search")
                            .param("keyword", "루나")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("검색 성공"))
                    .andExpect(jsonPath("$.data.artists[0].id").value(1L))
                    .andExpect(jsonPath("$.data.artists[0].name").value("루나"))
                    .andExpect(jsonPath("$.data.resultCount").value(3))
                    .andDo(document("search",
                            queryParameters(
                                    parameterWithName("keyword")
                                            .description("검색 키워드"),
                                    parameterWithName("page")
                                            .description("페이지 번호 (0부터 시작)").optional(),
                                    parameterWithName("size")
                                            .description("페이지 크기 (기본 20)").optional(),
                                    parameterWithName("sort")
                                            .description("정렬 조건 (기본 relevance,desc)").optional()
                            ),
                            relaxedResponseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지"),
                                    fieldWithPath("data.artists").type(ARRAY).description("아티스트 결과 (최대 5건)"),
                                    fieldWithPath("data.artists[].id").type(NUMBER).description("아티스트 ID"),
                                    fieldWithPath("data.artists[].name").type(STRING).description("아티스트 이름"),
                                    fieldWithPath("data.artists[].profileImageUrl").type(STRING).description("프로필 이미지 URL"),
                                    fieldWithPath("data.tracks.content").type(ARRAY).description("트랙 결과 목록"),
                                    fieldWithPath("data.tracks.content[].id").type(NUMBER).description("트랙 ID"),
                                    fieldWithPath("data.tracks.content[].title").type(STRING).description("트랙 제목"),
                                    fieldWithPath("data.tracks.content[].genre").type(STRING).description("장르"),
                                    fieldWithPath("data.tracks.content[].durationSec").type(NUMBER).description("재생 시간(초)"),
                                    fieldWithPath("data.albums.content").type(ARRAY).description("앨범 결과 목록"),
                                    fieldWithPath("data.albums.content[].id").type(NUMBER).description("앨범 ID"),
                                    fieldWithPath("data.albums.content[].title").type(STRING).description("앨범 제목"),
                                    fieldWithPath("data.albums.content[].coverImageUrl").type(STRING).description("앨범 커버 이미지 URL"),
                                    fieldWithPath("data.resultCount").type(NUMBER).description("전체 결과 개수")
                            )
                    ));
        }

        @Test
        @DisplayName("공백 keyword 시 400 반환")
        void search_blankKeyword() throws Exception {
            given(searchService.search(anyString(), any(), any()))
                    .willThrow(new BusinessException(SearchErrorCode.ERR_SEARCH_KEYWORD_BLANK));

            mockMvc.perform(get("/api/search")
                            .param("keyword", "   "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            SearchErrorCode.ERR_SEARCH_KEYWORD_BLANK.getMessage()))
                    .andDo(document("search-blank-keyword",
                            queryParameters(
                                    parameterWithName("keyword").description("검색 키워드 (공백)")
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            )
                    ));
        }
    }

    @Nested
    @WithMockUser
    @DisplayName("최근 검색 기록 조회 API")
    class GetRecentSearchHistories {

        @Test
        @DisplayName("최근 검색 기록 조회 성공 시 200 반환")
        void getRecentSearchHistories_success() throws Exception {
            given(searchHistoryService.getRecentSearchHistories(any()))
                    .willReturn(List.of("아이유", "루나"));

            mockMvc.perform(get("/api/search-histories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("최근 검색 기록 조회 성공"))
                    .andExpect(jsonPath("$.data[0]").value("아이유"))
                    .andExpect(jsonPath("$.data[1]").value("루나"))
                    .andDo(document("search-histories-get",
                            responseFields(
                                    baseSuccessResponseFields()
                            ).and(
                                    fieldWithPath("data").type(ARRAY).description("최근 검색 키워드 목록")
                            )
                    ));
        }

        @Test
        @DisplayName("기록이 없으면 빈 배열 반환")
        void getRecentSearchHistories_empty() throws Exception {
            given(searchHistoryService.getRecentSearchHistories(any()))
                    .willReturn(List.of());

            mockMvc.perform(get("/api/search-histories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    @Nested
    @WithMockUser
    @DisplayName("최근 검색 기록 전체 삭제 API")
    class DeleteAllRecentSearchHistories {

        @Test
        @DisplayName("전체 검색 기록 삭제 성공 시 200 반환")
        void deleteAll_success() throws Exception {
            doNothing().when(searchHistoryService).deleteAllRecentSearchHistories(any());

            mockMvc.perform(delete("/api/search-histories")
                            .with(csrf().asHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("최근 검색 기록 전체 삭제 성공"))
                    .andDo(document("search-histories-delete-all",
                            responseFields(
                                    baseSuccessResponseFields()
                            )
                    ));
        }
    }

    @Nested
    @WithMockUser
    @DisplayName("최근 검색 기록 개별 삭제 API")
    class DeleteRecentSearchHistory {

        @Test
        @DisplayName("개별 검색 기록 삭제 성공 시 200 반환")
        void deleteOne_success() throws Exception {
            doNothing().when(searchHistoryService)
                    .deleteRecentSearchHistory(any(), anyString());

            mockMvc.perform(delete("/api/search-histories/recent")
                            .with(csrf().asHeader())
                            .queryParam("keyword", "아이유"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("최근 검색 기록 삭제 성공"))
                    .andDo(document("search-histories-delete-one",
                            queryParameters(
                                    parameterWithName("keyword").description("삭제할 검색 키워드")
                            ),
                            responseFields(
                                    baseSuccessResponseFields()
                            )
                    ));
        }
    }

    // ===== RestDocs helper methods =====

    private FieldDescriptor[] baseSuccessResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                fieldWithPath("message").type(STRING).description("응답 메시지")
        };
    }

    private FieldDescriptor[] baseErrorResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                fieldWithPath("message").type(STRING).description("에러 메시지")
        };
    }
}
