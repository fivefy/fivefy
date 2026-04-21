package com.fivefy.domain.track.controller;

import com.fivefy.common.config.security.JwtUtil;
import com.fivefy.common.docs.RestDocsSupport;
import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.common.filter.LastActiveAtFilter;
import com.fivefy.domain.track.dto.request.TrackCommentCreateRequest;
import com.fivefy.domain.track.dto.response.TrackCommentResponse;
import com.fivefy.domain.track.enums.TrackCommentErrorCode;
import com.fivefy.domain.track.enums.TrackErrorCode;
import com.fivefy.domain.track.service.TrackCommentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TrackCommentController 웹 계층 테스트 및 REST Docs 문서화
 */
@WebMvcTest(TrackCommentController.class)
class TrackCommentControllerTest extends RestDocsSupport {

    @MockitoBean
    private TrackCommentService trackCommentService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private LastActiveAtFilter lastActiveAtFilter;

    @Nested
    @WithMockUser
    @DisplayName("트랙 댓글 작성 API")
    class CreateTrackComment {

        @Test
        @DisplayName("트랙 댓글 작성 성공")
        void createTrackComment_success() throws Exception {
            Long trackId = 1L;

            TrackCommentCreateRequest request =
                    new TrackCommentCreateRequest("댓글입니다");

            TrackCommentResponse response = new TrackCommentResponse(
                    10L,
                    1L,
                    trackId,
                    "댓글입니다",
                    LocalDateTime.of(2026, 4, 21, 12, 0, 0),
                    LocalDateTime.of(2026, 4, 21, 12, 0, 0)
            );

            given(trackCommentService.createTrackComment(eq(1L), eq(trackId), any(TrackCommentCreateRequest.class)))
                    .willReturn(response);

            mockMvc.perform(post("/api/tracks/{trackId}/comments", trackId)
                            .with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of())))
                            .with(csrf().asHeader())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("트랙 댓글 작성 성공"))
                    .andExpect(jsonPath("$.data.commentId").value(10L))
                    .andExpect(jsonPath("$.data.userId").value(1L))
                    .andExpect(jsonPath("$.data.trackId").value(trackId))
                    .andExpect(jsonPath("$.data.content").value("댓글입니다"))
                    .andDo(document("track-comments-create",
                            pathParameters(
                                    parameterWithName("trackId").description("트랙 ID")
                            ),
                            requestFields(
                                    fieldWithPath("content").type(STRING).description("댓글 내용")
                            ),
                            responseFields(
                                    baseSuccessResponseFields()
                            ).and(
                                    trackCommentResponseFields()
                            )
                    ));
        }

        @Test
        @DisplayName("트랙 댓글 작성 시 요청 검증 실패하면 400 반환")
        void createTrackComment_fail_validation() throws Exception {
            Long trackId = 1L;

            mockMvc.perform(post("/api/tracks/{trackId}/comments", trackId)
                            .with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of())))
                            .with(csrf().asHeader())
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "content": ""
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andDo(document("track-comments-create-invalid",
                            pathParameters(
                                    parameterWithName("trackId").description("트랙 ID")
                            ),
                            requestFields(
                                    fieldWithPath("content").type(STRING).description("댓글 내용 (값 없음)")
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            ).and(
                                    validationErrorResponseFields()
                            )
                    ));
        }

        @Test
        @DisplayName("트랙 댓글 작성 시 존재하지 않는 트랙이면 404 반환")
        void createTrackComment_fail_notFound() throws Exception {
            Long trackId = 999L;

            TrackCommentCreateRequest request =
                    new TrackCommentCreateRequest("댓글입니다");

            given(trackCommentService.createTrackComment(eq(1L), eq(trackId), any(TrackCommentCreateRequest.class)))
                    .willThrow(new BusinessException(TrackErrorCode.ERR_TRACK_NOT_FOUND));

            mockMvc.perform(post("/api/tracks/{trackId}/comments", trackId)
                            .with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of())))
                            .with(csrf().asHeader())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(
                            TrackErrorCode.ERR_TRACK_NOT_FOUND.getMessage()
                    ))
                    .andDo(document("track-comments-create-not-found",
                            pathParameters(
                                    parameterWithName("trackId").description("트랙 ID")
                            ),
                            requestFields(
                                    fieldWithPath("content").type(STRING).description("댓글 내용")
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            )
                    ));
        }
    }

    @Nested
    @WithMockUser
    @DisplayName("트랙 댓글 목록 조회 API")
    class GetTrackComments {

        @Test
        @DisplayName("트랙 댓글 목록 조회 성공")
        void getTrackComments_success() throws Exception {
            Long trackId = 1L;

            TrackCommentResponse content = new TrackCommentResponse(
                    10L,
                    1L,
                    trackId,
                    "댓글입니다",
                    LocalDateTime.of(2026, 4, 21, 12, 0, 0),
                    LocalDateTime.of(2026, 4, 21, 12, 0, 0)
            );

            PageResponse<TrackCommentResponse> response = PageResponse.from(
                    new PageImpl<>(List.of(content), PageRequest.of(0, 20), 1)
            );

            given(trackCommentService.getTrackComments(eq(trackId), any()))
                    .willReturn(response);

            mockMvc.perform(get("/api/tracks/{trackId}/comments", trackId)
                            .param("page", "0")
                            .param("size", "20")
                            .param("sort", "createdAt,desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("트랙 댓글 목록 조회 성공"))
                    .andExpect(jsonPath("$.data.content[0].commentId").value(10L))
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(20))
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.totalPages").value(1))
                    .andDo(document("track-comments-get-list",
                            pathParameters(
                                    parameterWithName("trackId").description("트랙 ID")
                            ),
                            queryParameters(
                                    parameterWithName("page").optional().description("페이지 번호"),
                                    parameterWithName("size").optional().description("페이지 크기"),
                                    parameterWithName("sort").optional().description("정렬 조건")
                            ),
                            responseFields(
                                    baseSuccessResponseFields()
                            ).and(
                                    trackCommentListResponseFields()
                            ).and(
                                    pageResponseFields()
                            )
                    ));
        }

        @Test
        @DisplayName("트랙 댓글 목록 조회 시 존재하지 않는 트랙이면 404 반환")
        void getTrackComments_fail_notFound() throws Exception {
            Long trackId = 999L;

            given(trackCommentService.getTrackComments(eq(trackId), any()))
                    .willThrow(new BusinessException(TrackErrorCode.ERR_TRACK_NOT_FOUND));

            mockMvc.perform(get("/api/tracks/{trackId}/comments", trackId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(
                            TrackErrorCode.ERR_TRACK_NOT_FOUND.getMessage()
                    ))
                    .andDo(document("track-comments-get-list-not-found",
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
    @DisplayName("트랙 댓글 수정 API")
    class UpdateTrackComment {

        @Test
        @DisplayName("트랙 댓글 수정 성공")
        void updateTrackComment_success() throws Exception {
            Long trackId = 1L;
            Long commentId = 10L;

            TrackCommentCreateRequest request =
                    new TrackCommentCreateRequest("수정된 댓글입니다");

            TrackCommentResponse response = new TrackCommentResponse(
                    commentId,
                    1L,
                    trackId,
                    "수정된 댓글입니다",
                    LocalDateTime.of(2026, 4, 21, 12, 0, 0),
                    LocalDateTime.of(2026, 4, 21, 13, 0, 0)
            );

            given(trackCommentService.updateTrackComment(eq(1L), eq(trackId), eq(commentId), any(TrackCommentCreateRequest.class)))
                    .willReturn(response);

            mockMvc.perform(patch("/api/tracks/{trackId}/comments/{commentId}", trackId, commentId)
                            .with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of())))
                            .with(csrf().asHeader())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("트랙 댓글 수정 성공"))
                    .andExpect(jsonPath("$.data.commentId").value(commentId))
                    .andExpect(jsonPath("$.data.content").value("수정된 댓글입니다"))
                    .andDo(document("track-comments-update",
                            pathParameters(
                                    parameterWithName("trackId").description("트랙 ID"),
                                    parameterWithName("commentId").description("댓글 ID")
                            ),
                            requestFields(
                                    fieldWithPath("content").type(STRING).description("댓글 내용")
                            ),
                            responseFields(
                                    baseSuccessResponseFields()
                            ).and(
                                    trackCommentResponseFields()
                            )
                    ));
        }

        @Test
        @DisplayName("트랙 댓글 수정 시 요청 검증 실패하면 400 반환")
        void updateTrackComment_fail_validation() throws Exception {
            Long trackId = 1L;
            Long commentId = 10L;

            mockMvc.perform(patch("/api/tracks/{trackId}/comments/{commentId}", trackId, commentId)
                            .with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of())))
                            .with(csrf().asHeader())
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "content": ""
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andDo(document("track-comments-update-invalid",
                            pathParameters(
                                    parameterWithName("trackId").description("트랙 ID"),
                                    parameterWithName("commentId").description("댓글 ID")
                            ),
                            requestFields(
                                    fieldWithPath("content").type(STRING).description("댓글 내용 (값 없음)")
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            ).and(
                                    validationErrorResponseFields()
                            )
                    ));
        }

        @Test
        @DisplayName("트랙 댓글 수정 시 존재하지 않는 댓글이면 404 반환")
        void updateTrackComment_fail_notFound() throws Exception {
            Long trackId = 1L;
            Long commentId = 999L;

            TrackCommentCreateRequest request =
                    new TrackCommentCreateRequest("수정된 댓글입니다");

            given(trackCommentService.updateTrackComment(eq(1L), eq(trackId), eq(commentId), any(TrackCommentCreateRequest.class)))
                    .willThrow(new BusinessException(TrackCommentErrorCode.ERR_TRACK_COMMENT_NOT_FOUND));

            mockMvc.perform(patch("/api/tracks/{trackId}/comments/{commentId}", trackId, commentId)
                            .with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of())))
                            .with(csrf().asHeader())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(
                            TrackCommentErrorCode.ERR_TRACK_COMMENT_NOT_FOUND.getMessage()
                    ))
                    .andDo(document("track-comments-update-not-found",
                            pathParameters(
                                    parameterWithName("trackId").description("트랙 ID"),
                                    parameterWithName("commentId").description("댓글 ID")
                            ),
                            requestFields(
                                    fieldWithPath("content").type(STRING).description("댓글 내용")
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            )
                    ));
        }

        @Test
        @DisplayName("트랙 댓글 수정 시 권한이 없으면 403 반환")
        void updateTrackComment_fail_forbidden() throws Exception {
            Long trackId = 1L;
            Long commentId = 10L;

            TrackCommentCreateRequest request =
                    new TrackCommentCreateRequest("수정된 댓글입니다");

            given(trackCommentService.updateTrackComment(eq(1L), eq(trackId), eq(commentId), any(TrackCommentCreateRequest.class)))
                    .willThrow(new BusinessException(TrackCommentErrorCode.ERR_FORBIDDEN_TRACK_COMMENT_UPDATE));

            mockMvc.perform(patch("/api/tracks/{trackId}/comments/{commentId}", trackId, commentId)
                            .with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of())))
                            .with(csrf().asHeader())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(
                            TrackCommentErrorCode.ERR_FORBIDDEN_TRACK_COMMENT_UPDATE.getMessage()
                    ))
                    .andDo(document("track-comments-update-forbidden",
                            pathParameters(
                                    parameterWithName("trackId").description("트랙 ID"),
                                    parameterWithName("commentId").description("댓글 ID")
                            ),
                            requestFields(
                                    fieldWithPath("content").type(STRING).description("댓글 내용")
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            )
                    ));
        }
    }

    @Nested
    @WithMockUser
    @DisplayName("트랙 댓글 삭제 API")
    class DeleteTrackComment {

        @Test
        @DisplayName("트랙 댓글 삭제 성공")
        void deleteTrackComment_success() throws Exception {
            Long trackId = 1L;
            Long commentId = 10L;

            mockMvc.perform(delete("/api/tracks/{trackId}/comments/{commentId}", trackId, commentId)
                            .with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of())))
                            .with(csrf().asHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("트랙 댓글 삭제 성공"))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andDo(document("track-comments-delete",
                            pathParameters(
                                    parameterWithName("trackId").description("트랙 ID"),
                                    parameterWithName("commentId").description("댓글 ID")
                            ),
                            responseFields(
                                    baseSuccessResponseFields()
                            )
                    ));
        }

        @Test
        @DisplayName("트랙 댓글 삭제 시 존재하지 않는 댓글이면 404 반환")
        void deleteTrackComment_fail_notFound() throws Exception {
            Long trackId = 1L;
            Long commentId = 999L;

            willThrow(new BusinessException(
                    TrackCommentErrorCode.ERR_TRACK_COMMENT_NOT_FOUND
            )).given(trackCommentService).deleteTrackComment(eq(1L), eq(trackId), eq(commentId));

            mockMvc.perform(delete("/api/tracks/{trackId}/comments/{commentId}", trackId, commentId)
                            .with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of())))
                            .with(csrf().asHeader()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(
                            TrackCommentErrorCode.ERR_TRACK_COMMENT_NOT_FOUND.getMessage()
                    ))
                    .andDo(document("track-comments-delete-not-found",
                            pathParameters(
                                    parameterWithName("trackId").description("트랙 ID"),
                                    parameterWithName("commentId").description("댓글 ID")
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            )
                    ));
        }

        @Test
        @DisplayName("트랙 댓글 삭제 시 권한이 없으면 403 반환")
        void deleteTrackComment_fail_forbidden() throws Exception {
            Long trackId = 1L;
            Long commentId = 10L;

            willThrow(new BusinessException(
                    TrackCommentErrorCode.ERR_FORBIDDEN_TRACK_COMMENT_DELETE
            )).given(trackCommentService).deleteTrackComment(eq(1L), eq(trackId), eq(commentId));

            mockMvc.perform(delete("/api/tracks/{trackId}/comments/{commentId}", trackId, commentId)
                            .with(authentication(new UsernamePasswordAuthenticationToken(1L, null, List.of())))
                            .with(csrf().asHeader()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(
                            TrackCommentErrorCode.ERR_FORBIDDEN_TRACK_COMMENT_DELETE.getMessage()
                    ))
                    .andDo(document("track-comments-delete-forbidden",
                            pathParameters(
                                    parameterWithName("trackId").description("트랙 ID"),
                                    parameterWithName("commentId").description("댓글 ID")
                            ),
                            responseFields(
                                    baseErrorResponseFields()
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
                fieldWithPath("message").type(STRING).description("에러 메시지")
        };
    }

    private FieldDescriptor[] validationErrorResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("data").type(ARRAY).description("상세 에러 내역"),
                fieldWithPath("data[].field").type(STRING).description("에러가 발생한 필드명"),
                fieldWithPath("data[].message").type(STRING).description("에러 상세 사유")
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

    private FieldDescriptor[] trackCommentResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("data.commentId").type(NUMBER).description("댓글 ID"),
                fieldWithPath("data.userId").type(NUMBER).description("댓글 작성자 ID"),
                fieldWithPath("data.trackId").type(NUMBER).description("트랙 ID"),
                fieldWithPath("data.content").type(STRING).description("댓글 내용"),
                fieldWithPath("data.createdAt").type(STRING).description("댓글 작성 시각"),
                fieldWithPath("data.updatedAt").type(STRING).description("댓글 수정 시각")
        };
    }

    private FieldDescriptor[] trackCommentListResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("data.content").type(ARRAY).description("트랙 댓글 목록"),
                fieldWithPath("data.content[].commentId").type(NUMBER).description("댓글 ID"),
                fieldWithPath("data.content[].userId").type(NUMBER).description("댓글 작성자 ID"),
                fieldWithPath("data.content[].trackId").type(NUMBER).description("트랙 ID"),
                fieldWithPath("data.content[].content").type(STRING).description("댓글 내용"),
                fieldWithPath("data.content[].createdAt").type(STRING).description("댓글 작성 시각"),
                fieldWithPath("data.content[].updatedAt").type(STRING).description("댓글 수정 시각")
        };
    }
}