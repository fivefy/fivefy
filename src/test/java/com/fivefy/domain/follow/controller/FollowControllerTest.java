package com.fivefy.domain.follow.controller;

import com.fivefy.common.config.security.JwtUtil;
import com.fivefy.common.docs.RestDocsSupport;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.follow.dto.request.FollowCreateRequest;
import com.fivefy.domain.follow.dto.response.FollowCreateResponse;
import com.fivefy.domain.follow.dto.response.FollowGetResponse;
import com.fivefy.domain.follow.enums.FollowErrorCode;
import com.fivefy.domain.follow.service.FollowService;
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

@WebMvcTest(FollowController.class)
class FollowControllerTest extends RestDocsSupport {

    @MockitoBean
    private FollowService followService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    private static final Long ARTIST_ID = 2L;
    private static final Long FOLLOW_ID = 1L;

    @Nested
    @WithMockUser
    @DisplayName("팔로우 등록 API")
    class CreateFollow {

        @Test
        @DisplayName("팔로우 등록 성공 시 201 반환")
        void createFollow_success() throws Exception {
            FollowCreateRequest request = new FollowCreateRequest(ARTIST_ID);
            FollowCreateResponse response = new FollowCreateResponse(
                    FOLLOW_ID, ARTIST_ID, true,
                    LocalDateTime.of(2026, 5, 13, 10, 0, 0)
            );

            given(followService.createFollow(any(), eq(ARTIST_ID))).willReturn(response);

            mockMvc.perform(post("/api/follows")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("팔로우 등록 성공"))
                    .andExpect(jsonPath("$.data.artistId").value(ARTIST_ID))
                    .andDo(document("follow-create",
                            requestFields(
                                    followCreateRequestFields()
                            ),
                            responseFields(
                                    baseSuccessResponseFields()
                            ).and(
                                    followCreateResponseFields()
                            )
                    ));
        }

        @Test
        @DisplayName("artistId null 시 400 반환")
        void createFollow_invalidRequest() throws Exception {
            FollowCreateRequest request = new FollowCreateRequest(null);

            mockMvc.perform(post("/api/follows")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andDo(document("follow-create-invalid",
                            requestFields(
                                    fieldWithPath("artistId").type(NULL).description("아티스트 ID (값 없음)")
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            ).and(
                                    validationErrorResponseFields()
                            )
                    ));
        }

        @Test
        @DisplayName("중복 팔로우 시 409 반환")
        void createFollow_duplicate() throws Exception {
            FollowCreateRequest request = new FollowCreateRequest(ARTIST_ID);
            given(followService.createFollow(any(), eq(ARTIST_ID)))
                    .willThrow(new BusinessException(FollowErrorCode.ERR_FOLLOW_ALREADY_EXISTS));

            mockMvc.perform(post("/api/follows")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(
                            FollowErrorCode.ERR_FOLLOW_ALREADY_EXISTS.getMessage()))
                    .andDo(document("follow-create-duplicate",
                            requestFields(
                                    followCreateRequestFields()
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            )
                    ));
        }
    }

    @Nested
    @WithMockUser
    @DisplayName("팔로우 목록 조회 API")
    class GetFollows {

        @Test
        @DisplayName("팔로우 목록 조회 성공 시 200 반환")
        void getFollows_success() throws Exception {
            FollowGetResponse followGetResponse = new FollowGetResponse(
                    FOLLOW_ID, ARTIST_ID, true
            );
            PageImpl<FollowGetResponse> page = new PageImpl<>(
                    List.of(followGetResponse), PageRequest.of(0, 20), 1
            );

            given(followService.getFollows(any(), any())).willReturn(page);

            mockMvc.perform(get("/api/follows")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("팔로우 목록 조회 성공"))
                    .andExpect(jsonPath("$.data.content[0].artistId").value(ARTIST_ID))
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andDo(document("follow-list",
                            queryParameters(
                                    parameterWithName("page")
                                            .description("페이지 번호 (0부터 시작)").optional(),
                                    parameterWithName("size")
                                            .description("페이지 크기 (기본 20)").optional(),
                                    parameterWithName("sort")
                                            .description("정렬 조건 (기본 artistId,asc)").optional()
                            ),
                            responseFields(
                                    baseSuccessResponseFields()
                            ).and(
                                    pageResponseFields()
                            ).and(
                                    followListContentResponseFields()
                            )
                    ));
        }
    }

    @Nested
    @WithMockUser
    @DisplayName("팔로우 취소 API")
    class DeleteFollow {

        @Test
        @DisplayName("팔로우 취소 성공 시 200 반환")
        void deleteFollow_success() throws Exception {
            doNothing().when(followService).deleteFollow(any(), eq(ARTIST_ID));

            mockMvc.perform(delete("/api/follows/{artistId}", ARTIST_ID)
                            .with(csrf().asHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("팔로우 취소 성공"))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andDo(document("follow-delete",
                            pathParameters(
                                    parameterWithName("artistId").description("취소할 아티스트 ID")
                            ),
                            responseFields(
                                    baseSuccessResponseFields()
                            )
                    ));
        }

        @Test
        @DisplayName("존재하지 않는 팔로우 취소 시 404 반환")
        void deleteFollow_notFound() throws Exception {
            doThrow(new BusinessException(FollowErrorCode.ERR_FOLLOW_NOT_FOUND))
                    .when(followService).deleteFollow(any(), eq(ARTIST_ID));

            mockMvc.perform(delete("/api/follows/{artistId}", ARTIST_ID)
                            .with(csrf().asHeader()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(
                            FollowErrorCode.ERR_FOLLOW_NOT_FOUND.getMessage()))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andDo(document("follow-delete-not-found",
                            pathParameters(
                                    parameterWithName("artistId").description("취소할 아티스트 ID")
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            )
                    ));
        }
    }

    @Nested
    @WithMockUser
    @DisplayName("알림 설정 토글 API")
    class ToggleNotification {

        @Test
        @DisplayName("알림 설정 토글 성공 시 200 반환")
        void toggleNotification_success() throws Exception {
            doNothing().when(followService).toggleNotification(any(), eq(ARTIST_ID));

            mockMvc.perform(patch("/api/follows/{artistId}/notifications", ARTIST_ID)
                            .with(csrf().asHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("알림 설정 변경 성공"))
                    .andDo(document("follow-toggle-notification",
                            pathParameters(
                                    parameterWithName("artistId").description("아티스트 ID")
                            ),
                            responseFields(
                                    baseSuccessResponseFields()
                            )
                    ));
        }

        @Test
        @DisplayName("존재하지 않는 팔로우 토글 시 404 반환")
        void toggleNotification_notFound() throws Exception {
            doThrow(new BusinessException(FollowErrorCode.ERR_FOLLOW_NOT_FOUND))
                    .when(followService).toggleNotification(any(), eq(ARTIST_ID));

            mockMvc.perform(patch("/api/follows/{artistId}/notifications", ARTIST_ID)
                            .with(csrf().asHeader()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(
                            FollowErrorCode.ERR_FOLLOW_NOT_FOUND.getMessage()))
                    .andDo(document("follow-toggle-notification-not-found",
                            pathParameters(
                                    parameterWithName("artistId").description("아티스트 ID")
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

    private FieldDescriptor[] followCreateRequestFields() {
        return new FieldDescriptor[]{
                fieldWithPath("artistId").type(NUMBER).description("팔로우할 아티스트 ID")
        };
    }

    private FieldDescriptor[] followCreateResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("data.id").type(NUMBER).description("생성된 팔로우 ID"),
                fieldWithPath("data.artistId").type(NUMBER).description("팔로우한 아티스트 ID"),
                fieldWithPath("data.notificationEnabled").type(BOOLEAN).description("알림 설정 여부"),
                fieldWithPath("data.createdAt").type(STRING).description("팔로우 생성 시각")
        };
    }

    private FieldDescriptor[] followListContentResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("data.content").type(ARRAY).description("팔로우 목록"),
                fieldWithPath("data.content[].id").type(NUMBER).description("팔로우 ID"),
                fieldWithPath("data.content[].artistId").type(NUMBER).description("아티스트 ID"),
                fieldWithPath("data.content[].notificationEnabled").type(BOOLEAN).description("알림 설정 여부")
        };
    }
}
