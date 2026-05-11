package com.fivefy.domain.track.controller;

import com.fivefy.common.config.security.JwtUtil;
import com.fivefy.common.docs.RestDocsSupport;
import com.fivefy.common.dto.response.SliceResponse;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.common.filter.LastActiveAtFilter;
import com.fivefy.domain.track.dto.response.PublicTrackListResponse;
import com.fivefy.domain.track.dto.response.TrackCommentResponse;
import com.fivefy.domain.track.dto.response.TrackDetailResponse;
import com.fivefy.domain.track.enums.TrackErrorCode;
import com.fivefy.domain.track.enums.TrackType;
import com.fivefy.domain.track.service.TrackService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TrackController 웹 계층 테스트 및 REST Docs 문서화
 */
@WebMvcTest(TrackController.class)
class TrackControllerTest extends RestDocsSupport {

    @MockitoBean
    private TrackService trackService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private LastActiveAtFilter lastActiveAtFilter;

    @Nested
    @WithMockUser
    @DisplayName("트랙 상세 조회 API")
    class GetTrack {

        @Test
        @DisplayName("트랙 상세 조회 성공")
        void getTrack_success() throws Exception {
            Long trackId = 1L;

            TrackDetailResponse response = new TrackDetailResponse(
                    trackId,
                    TrackType.OFFICIAL_RELEASE,
                    10L,
                    "아이유",
                    100L,
                    "Palette",
                    2L,
                    "밤편지",
                    "가사",
                    "BALLAD",
                    "https://example.com/audio.mp3",
                    230L,
                    "feat. 10cm",
                    1200L,
                    LocalDateTime.of(2026, 5, 1, 18, 0, 0),
                    List.of(
                            new TrackCommentResponse(
                                    10L,
                                    3L,
                                    1000L,
                                    "좋아요",
                                    LocalDateTime.of(2026, 4, 22, 13, 0, 0),
                                    LocalDateTime.of(2026, 4, 22, 13, 0, 0)
                            )
                    )
            );

            given(trackService.getTrack(trackId)).willReturn(response);

            mockMvc.perform(get("/api/tracks/{trackId}", trackId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("트랙 상세 조회 성공"))
                    .andExpect(jsonPath("$.data.trackId").value(trackId))
                    .andExpect(jsonPath("$.data.title").value("밤편지"))
                    .andExpect(jsonPath("$.data.comments[0].commentId").value(10L))
                    .andExpect(jsonPath("$.data.comments[0].userId").value(3L))
                    .andExpect(jsonPath("$.data.comments[0].trackId").value(1000L))
                    .andExpect(jsonPath("$.data.comments[0].content").value("좋아요"))
                    .andDo(document("tracks-get",
                            pathParameters(
                                    parameterWithName("trackId").description("트랙 ID")
                            ),
                            responseFields(
                                    baseSuccessResponseFields()
                            ).and(
                                    trackDetailResponseFields()
                            )
                    ));
        }

        @Test
        @DisplayName("트랙 상세 조회 시 존재하지 않는 트랙이면 404 반환")
        void getTrack_fail_notFound() throws Exception {
            Long trackId = 999L;

            given(trackService.getTrack(trackId))
                    .willThrow(new BusinessException(TrackErrorCode.ERR_TRACK_NOT_FOUND));

            mockMvc.perform(get("/api/tracks/{trackId}", trackId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(
                            TrackErrorCode.ERR_TRACK_NOT_FOUND.getMessage()
                    ))
                    .andDo(document("tracks-get-not-found",
                            pathParameters(
                                    parameterWithName("trackId").description("트랙 ID")
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            )
                    ));
        }

        @Test
        @DisplayName("트랙 상세 조회 시 비공개 트랙이면 404 반환")
        void getTrack_fail_unpublished() throws Exception {
            Long trackId = 2L;

            given(trackService.getTrack(trackId))
                    .willThrow(new BusinessException(
                            TrackErrorCode.ERR_TRACK_NOT_FOUND
                    ));

            mockMvc.perform(get("/api/tracks/{trackId}", trackId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(
                            TrackErrorCode.ERR_TRACK_NOT_FOUND.getMessage()
                    ))
                    .andDo(document("tracks-get-unpublished",
                            pathParameters(
                                    parameterWithName("trackId").description("트랙 ID")
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            )
                    ));
        }
    }

    @Nested
    @WithMockUser
    @DisplayName("공개 트랙 목록 조회 API")
    class GetPublicTracks {

        @Test
        @DisplayName("공개 트랙 목록 조회 성공")
        void getPublicTracks_success() throws Exception {
            PublicTrackListResponse content = new PublicTrackListResponse(
                    1L,
                    TrackType.OFFICIAL_RELEASE,
                    "밤편지",
                    10L,
                    "아이유",
                    100L,
                    "Palette",
                    230L,
                    1200L,
                    LocalDateTime.of(2026, 5, 1, 18, 0, 0)
            );

            SliceResponse<PublicTrackListResponse> response = SliceResponse.from(
                    new SliceImpl<>(List.of(content), PageRequest.of(0, 20), true)
            );

            given(trackService.getPublicTracks(any(Pageable.class))).willReturn(response);

            mockMvc.perform(get("/api/tracks")
                            .param("page", "0")
                            .param("size", "20")
                            .param("sort", "publishedAt,desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("공개 트랙 목록 조회 성공"))
                    .andExpect(jsonPath("$.data.content[0].trackId").value(1L))
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(20))
                    .andExpect(jsonPath("$.data.hasNext").value(true))
                    .andDo(document("tracks-get-list",
                            queryParameters(
                                    parameterWithName("page").optional().description("페이지 번호"),
                                    parameterWithName("size").optional().description("페이지 크기"),
                                    parameterWithName("sort").optional().description("정렬 조건")
                            ),
                            responseFields(
                                    baseSuccessResponseFields()
                            ).and(
                                    publicTrackListResponseFields()
                            ).and(
                                    sliceResponseFields()
                            )
                    ));
        }
    }

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
                fieldWithPath("message").type(STRING).description("에러 메시지"),
        };
    }

    private FieldDescriptor[] pageResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("data.page").type(NUMBER).description("현재 페이지"),
                fieldWithPath("data.size").type(NUMBER).description("페이지 크기"),
                fieldWithPath("data.totalElements").type(NUMBER).description("전체 데이터 수"),
                fieldWithPath("data.totalPages").type(NUMBER).description("전체 페이지 수")
        };
    }

    private FieldDescriptor[] sliceResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("data.page").type(NUMBER).description("현재 페이지"),
                fieldWithPath("data.size").type(NUMBER).description("페이지 크기"),
                fieldWithPath("data.hasNext").type(BOOLEAN).description("다음 페이지 존재 여부")
        };
    }

    private FieldDescriptor[] trackDetailResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("data.trackId").type(NUMBER).description("트랙 ID"),
                fieldWithPath("data.trackType").type(STRING).description("트랙 유형"),
                fieldWithPath("data.artistId").type(NUMBER).description("아티스트 ID").optional(),
                fieldWithPath("data.artistName").type(STRING).description("아티스트 이름").optional(),
                fieldWithPath("data.albumId").type(NUMBER).description("앨범 ID").optional(),
                fieldWithPath("data.albumTitle").type(STRING).description("앨범 제목").optional(),
                fieldWithPath("data.trackNumber").type(NUMBER).description("트랙 번호").optional(),
                fieldWithPath("data.title").type(STRING).description("트랙 제목"),
                fieldWithPath("data.lyrics").type(STRING).description("가사").optional(),
                fieldWithPath("data.genre").type(STRING).description("장르"),
                fieldWithPath("data.audioUrl").type(STRING).description("오디오 URL"),
                fieldWithPath("data.durationSec").type(NUMBER).description("재생 시간(초)"),
                fieldWithPath("data.featuredArtistText").type(STRING).description("피처링 아티스트 텍스트").optional(),
                fieldWithPath("data.playCount").type(NUMBER).description("재생 횟수"),
                fieldWithPath("data.publishedAt").type(STRING).description("공개 시각"),
                fieldWithPath("data.comments").type(ARRAY).description("최신 트랙 댓글 목록"),
                fieldWithPath("data.comments[].commentId").type(NUMBER).description("댓글 ID"),
                fieldWithPath("data.comments[].userId").type(NUMBER).description("댓글 작성자 ID"),
                fieldWithPath("data.comments[].trackId").type(NUMBER).description("트랙 ID"),
                fieldWithPath("data.comments[].content").type(STRING).description("댓글 내용"),
                fieldWithPath("data.comments[].createdAt").type(STRING).description("댓글 작성 시각"),
                fieldWithPath("data.comments[].updatedAt").type(STRING).description("댓글 수정 시각")
        };
    }

    private FieldDescriptor[] publicTrackListResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("data.content").type(ARRAY).description("공개 트랙 목록"),
                fieldWithPath("data.content[].trackId").type(NUMBER).description("트랙 ID"),
                fieldWithPath("data.content[].trackType").type(STRING).description("트랙 유형"),
                fieldWithPath("data.content[].title").type(STRING).description("트랙 제목"),
                fieldWithPath("data.content[].artistId").type(NUMBER).description("아티스트 ID").optional(),
                fieldWithPath("data.content[].artistName").type(STRING).description("아티스트 이름").optional(),
                fieldWithPath("data.content[].albumId").type(NUMBER).description("앨범 ID").optional(),
                fieldWithPath("data.content[].albumTitle").type(STRING).description("앨범 제목").optional(),
                fieldWithPath("data.content[].durationSec").type(NUMBER).description("재생 시간(초)"),
                fieldWithPath("data.content[].playCount").type(NUMBER).description("재생 횟수"),
                fieldWithPath("data.content[].publishedAt").type(STRING).description("공개 시각")
        };
    }

    private FieldDescriptor[] validationErrorResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("data").type(ARRAY).description("상세 에러 내역"),
                fieldWithPath("data[].field").type(STRING).description("에러가 발생한 필드명"),
                fieldWithPath("data[].message").type(STRING).description("에러 상세 사유")
        };
    }
}