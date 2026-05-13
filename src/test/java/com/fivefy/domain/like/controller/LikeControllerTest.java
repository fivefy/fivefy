package com.fivefy.domain.like.controller;

import com.fivefy.common.config.security.JwtUtil;
import com.fivefy.common.docs.RestDocsSupport;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.common.filter.LastActiveAtFilter;
import com.fivefy.domain.like.dto.request.LikeCreateRequest;
import com.fivefy.domain.like.dto.response.LikeCreateResponse;
import com.fivefy.domain.like.dto.response.LikeGetResponse;
import com.fivefy.domain.like.enums.LikeErrorCode;
import com.fivefy.domain.like.enums.TargetType;
import com.fivefy.domain.like.service.LikeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LikeController.class)
class LikeControllerTest extends RestDocsSupport {

    @MockitoBean
    private LikeService likeService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    private static final Long TRACK_ID = 2L;
    private static final Long LIKE_ID = 4L;

    @Nested
    @WithMockUser
    @DisplayName("좋아요 등록 API")
    class CreateLike {

        @Test
        @DisplayName("좋아요 등록 성공 시 201 반환")
        void createLike_success() throws Exception {
            LikeCreateRequest request = new LikeCreateRequest(TRACK_ID, TargetType.TRACK);
            LikeCreateResponse response = new LikeCreateResponse(
                    LIKE_ID, TRACK_ID, TargetType.TRACK,
                    LocalDateTime.of(2026, 5, 13, 10, 0, 0)
            );

            given(likeService.createLike(eq(TRACK_ID), eq(TargetType.TRACK), any()))
                    .willReturn(response);

            mockMvc.perform(post("/api/likes")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("좋아요 등록 성공"))
                    .andExpect(jsonPath("$.data.targetId").value(TRACK_ID))
                    .andExpect(jsonPath("$.data.targetType").value("TRACK"))
                    .andDo(document("like-create",
                            requestFields(
                                    likeCreateRequestFields()
                            ),
                            responseFields(
                                    baseSuccessResponseFields()
                            ).and(
                                    likeCreateResponseFields()
                            )
                    ));
        }

        @Test
        @DisplayName("targetId null 시 400 반환")
        void createLike_targetIdNull() throws Exception {
            LikeCreateRequest request = new LikeCreateRequest(null, TargetType.TRACK);

            mockMvc.perform(post("/api/likes")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andDo(document("like-create-invalid-targetId",
                            requestFields(
                                    fieldWithPath("targetId").type(NULL).description("좋아요 대상 ID (값 없음)"),
                                    fieldWithPath("targetType").type(STRING).description("대상 타입 (TRACK / ALBUM)")
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            ).and(
                                    validationErrorResponseFields()
                            )
                    ));
        }

        @Test
        @DisplayName("targetType null 시 400 반환")
        void createLike_targetTypeNull() throws Exception {
            LikeCreateRequest request = new LikeCreateRequest(TRACK_ID, null);

            mockMvc.perform(post("/api/likes")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andDo(document("like-create-invalid-targetType",
                            requestFields(
                                    fieldWithPath("targetId").type(NUMBER).description("좋아요 대상 ID"),
                                    fieldWithPath("targetType").type(NULL).description("대상 타입 (값 없음)")
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            ).and(
                                    validationErrorResponseFields()
                            )
                    ));
        }

        @Test
        @DisplayName("중복 좋아요 시 409 반환")
        void createLike_duplicate() throws Exception {
            LikeCreateRequest request = new LikeCreateRequest(TRACK_ID, TargetType.TRACK);
            given(likeService.createLike(eq(TRACK_ID), eq(TargetType.TRACK), any()))
                    .willThrow(new BusinessException(LikeErrorCode.ERR_LIKE_ALREADY_EXISTS));

            mockMvc.perform(post("/api/likes")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(
                            LikeErrorCode.ERR_LIKE_ALREADY_EXISTS.getMessage()))
                    .andDo(document("like-create-duplicate",
                            requestFields(
                                    likeCreateRequestFields()
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            )
                    ));
        }
    }

    @Nested
    @WithMockUser
    @DisplayName("좋아요 목록 조회 API")
    class GetLikes {

        @Test
        @DisplayName("좋아요 목록 조회 성공 시 200 반환")
        void getLikes_success() throws Exception {
            LikeGetResponse likeGetResponse = new LikeGetResponse(
                    LIKE_ID, TRACK_ID, TargetType.TRACK, "Love poem", "아이유",
                    LocalDateTime.of(2026, 5, 13, 10, 0, 0)
            );
            PageImpl<LikeGetResponse> page = new PageImpl<>(
                    List.of(likeGetResponse), PageRequest.of(0, 20), 1
            );

            given(likeService.getLikes(any(), any(), any())).willReturn(page);

            mockMvc.perform(get("/api/likes")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("좋아요 목록 조회 성공"))
                    .andExpect(jsonPath("$.data.content[0].targetId").value(TRACK_ID))
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andDo(document("like-list",
                            queryParameters(
                                    parameterWithName("targetType")
                                            .description("대상 타입 필터 (TRACK / ALBUM, 선택)").optional(),
                                    parameterWithName("page")
                                            .description("페이지 번호 (0부터 시작)").optional(),
                                    parameterWithName("size")
                                            .description("페이지 크기 (기본 20)").optional()
                            ),
                            responseFields(
                                    baseSuccessResponseFields()
                            ).and(
                                    pageResponseFields()
                            ).and(
                                    likeListContentResponseFields()
                            )
                    ));
        }

        @Test
        @DisplayName("targetType 필터링 조회 성공 시 200 반환")
        void getLikes_withTargetTypeFilter() throws Exception {
            LikeGetResponse likeGetResponse = new LikeGetResponse(
                    LIKE_ID, TRACK_ID, TargetType.TRACK, "Love poem", "아이유",
                    LocalDateTime.of(2026, 5, 13, 10, 0, 0)
            );
            PageImpl<LikeGetResponse> page = new PageImpl<>(
                    List.of(likeGetResponse), PageRequest.of(0, 20), 1
            );

            given(likeService.getLikes(any(), eq(TargetType.TRACK), any())).willReturn(page);

            mockMvc.perform(get("/api/likes")
                            .param("targetType", "TRACK")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].targetType").value("TRACK"));
        }
    }

    @Nested
    @WithMockUser
    @DisplayName("좋아요 취소 API")
    class DeleteLike {

        @Test
        @DisplayName("좋아요 취소 성공 시 200 반환")
        void deleteLike_success() throws Exception {
            doNothing().when(likeService).deleteLike(any(), eq(LIKE_ID));

            mockMvc.perform(delete("/api/likes/{likeId}", LIKE_ID)
                            .with(csrf().asHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("좋아요 취소 성공"))
                    .andDo(document("like-delete",
                            pathParameters(
                                    parameterWithName("likeId").description("취소할 좋아요 ID")
                            ),
                            responseFields(
                                    baseSuccessResponseFields()
                            )
                    ));
        }

        @Test
        @DisplayName("존재하지 않는 좋아요 취소 시 404 반환")
        void deleteLike_notFound() throws Exception {
            doThrow(new BusinessException(LikeErrorCode.ERR_LIKE_NOT_FOUND))
                    .when(likeService).deleteLike(any(), eq(LIKE_ID));

            mockMvc.perform(delete("/api/likes/{likeId}", LIKE_ID)
                            .with(csrf().asHeader()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(
                            LikeErrorCode.ERR_LIKE_NOT_FOUND.getMessage()))
                    .andDo(document("like-delete-not-found",
                            pathParameters(
                                    parameterWithName("likeId").description("취소할 좋아요 ID")
                            ),
                            responseFields(
                                    baseErrorResponseFields()
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

    private FieldDescriptor[] likeCreateRequestFields() {
        return new FieldDescriptor[]{
                fieldWithPath("targetId").type(NUMBER).description("좋아요 대상 ID"),
                fieldWithPath("targetType").type(STRING).description("대상 타입 (TRACK / ALBUM)")
        };
    }

    private FieldDescriptor[] likeCreateResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("data.id").type(NUMBER).description("생성된 좋아요 ID"),
                fieldWithPath("data.targetId").type(NUMBER).description("좋아요 대상 ID"),
                fieldWithPath("data.targetType").type(STRING).description("대상 타입 (TRACK / ALBUM)"),
                fieldWithPath("data.createdAt").type(STRING).description("좋아요 생성 시각")
        };
    }

    private FieldDescriptor[] likeListContentResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("data.content").type(ARRAY).description("좋아요 목록"),
                fieldWithPath("data.content[].id").type(NUMBER).description("좋아요 ID"),
                fieldWithPath("data.content[].targetId").type(NUMBER).description("좋아요 대상 ID"),
                fieldWithPath("data.content[].targetType").type(STRING).description("대상 타입"),
                fieldWithPath("data.content[].targetName").type(STRING).description("대상 이름 (트랙명/앨범명)"),
                fieldWithPath("data.content[].artistName").type(STRING).description("아티스트 이름"),
                fieldWithPath("data.content[].createdAt").type(STRING).description("좋아요 생성 시각")
        };
    }
}
