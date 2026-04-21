package com.fivefy.domain.album.controller;

import com.fivefy.common.config.security.JwtUtil;
import com.fivefy.common.docs.RestDocsSupport;
import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.common.filter.LastActiveAtFilter;
import com.fivefy.domain.album.dto.request.AlbumApplicationCreateRequest;
import com.fivefy.domain.album.dto.request.AlbumApplicationRejectRequest;
import com.fivefy.domain.album.dto.response.*;
import com.fivefy.domain.album.enums.AlbumApplicationErrorCode;
import com.fivefy.domain.album.enums.AlbumErrorCode;
import com.fivefy.domain.album.service.AlbumService;
import com.fivefy.domain.artist.enums.ArtistErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AlbumController 웹 계층 테스트 및 REST Docs 문서화
 */
@WebMvcTest(AlbumController.class)
class AlbumControllerTest extends RestDocsSupport {

    @MockitoBean
    private AlbumService albumService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private LastActiveAtFilter lastActiveAtFilter;

    @Nested
    @WithMockUser
    @DisplayName("앨범 등록 신청 생성 API")
    class CreateAlbumApplication {

        @Test
        @DisplayName("앨범 등록 신청 생성 성공")
        void createAlbumApplication_success() throws Exception {
            AlbumApplicationCreateRequest request = new AlbumApplicationCreateRequest(
                    10L,
                    "Love poem",
                    "앨범 설명",
                    "https://example.com/cover.jpg",
                    3
            );

            AlbumApplicationResponse response = new AlbumApplicationResponse(
                    1L,
                    10L,
                    "Love poem",
                    ApplicationStatus.PENDING,
                    LocalDateTime.of(2026, 4, 16, 16, 0, 0)
            );

            given(albumService.createAlbumApplication(any(), any(AlbumApplicationCreateRequest.class)))
                    .willReturn(response);

            mockMvc.perform(post("/api/album-applications")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("앨범 등록 신청 성공"))
                    .andExpect(jsonPath("$.data.applicationId").value(1L))
                    .andExpect(jsonPath("$.data.artistId").value(10L))
                    .andDo(document("album-applications-create",
                            requestFields(
                                    albumApplicationCreateRequestFields()
                            ),
                            responseFields(
                                    baseSuccessResponseFields()
                            ).and(
                                    albumApplicationResponseFields()
                            )
                    ));
        }

        @Test
        @DisplayName("제목 없이 생성 요청 시 400 반환")
        void createAlbumApplication_fail_withoutTitle() throws Exception {
            AlbumApplicationCreateRequest request = new AlbumApplicationCreateRequest(
                    10L,
                    "",
                    "앨범 설명",
                    "https://example.com/cover.jpg",
                    3
            );

            mockMvc.perform(post("/api/album-applications")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andDo(document("album-applications-create-invalid",
                            requestFields(
                                    fieldWithPath("artistId").type(NUMBER).description("아티스트 ID"),
                                    fieldWithPath("title").type(STRING).description("앨범 제목 (값 없음)"),
                                    fieldWithPath("description").type(STRING).description("앨범 설명"),
                                    fieldWithPath("coverImageUrl").type(STRING).description("커버 이미지 URL"),
                                    fieldWithPath("publishDelayDays").type(NUMBER).description("공개 예약 일수 (0~7)")
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            ).and(
                                    validationErrorResponseFields()
                            )
                    ));
        }

        @Test
        @DisplayName("중복 신청이면 409 반환")
        void createAlbumApplication_fail_whenAlreadyExists() throws Exception {
            AlbumApplicationCreateRequest request = new AlbumApplicationCreateRequest(
                    10L,
                    "Love poem",
                    "앨범 설명",
                    "https://example.com/cover.jpg",
                    3
            );

            given(albumService.createAlbumApplication(any(), any(AlbumApplicationCreateRequest.class)))
                    .willThrow(new BusinessException(
                            AlbumApplicationErrorCode.ERR_ALBUM_APPLICATION_ALREADY_EXISTS
                    ));

            mockMvc.perform(post("/api/album-applications")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(
                            AlbumApplicationErrorCode.ERR_ALBUM_APPLICATION_ALREADY_EXISTS.getMessage()
                    ))
                    .andDo(document("album-applications-create-duplicate",
                            requestFields(
                                    albumApplicationCreateRequestFields()
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            )
                    ));
        }
    }

    @Nested
    @WithMockUser
    @DisplayName("내 앨범 등록 신청 목록 조회 API")
    class GetMyAlbumApplications {

        @Test
        @DisplayName("내 앨범 등록 신청 목록 조회 성공")
        void getMyAlbumApplications_success() throws Exception {
            List<AlbumApplicationResponse> response = List.of(
                    new AlbumApplicationResponse(
                            2L,
                            10L,
                            "두 번째 신청",
                            ApplicationStatus.PENDING,
                            LocalDateTime.of(2026, 4, 16, 16, 0, 0)
                    ),
                    new AlbumApplicationResponse(
                            1L,
                            10L,
                            "첫 번째 신청",
                            ApplicationStatus.PENDING,
                            LocalDateTime.of(2026, 4, 15, 16, 0, 0)
                    )
            );

            given(albumService.getMyAlbumApplications(any())).willReturn(response);

            mockMvc.perform(get("/api/album-applications/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("내 앨범 등록 신청 목록 조회 성공"))
                    .andExpect(jsonPath("$.data[0].applicationId").value(2L))
                    .andExpect(jsonPath("$.data[0].title").value("두 번째 신청"))
                    .andDo(document("album-applications-me-get",
                            responseFields(
                                    baseSuccessResponseFields()
                            ).and(
                                    fieldWithPath("data").type(ARRAY).description("내 앨범 등록 신청 목록"),
                                    fieldWithPath("data[].applicationId").type(NUMBER).description("신청 ID"),
                                    fieldWithPath("data[].artistId").type(NUMBER).description("아티스트 ID"),
                                    fieldWithPath("data[].title").type(STRING).description("앨범 제목"),
                                    fieldWithPath("data[].status").type(STRING).description("신청 상태"),
                                    fieldWithPath("data[].createdAt").type(STRING).description("신청 생성 시각")
                            )
                    ));
        }
    }

    @Nested
    @WithMockUser
    @DisplayName("앨범 등록 신청 상세 조회 API")
    class GetAlbumApplication {

        @Test
        @DisplayName("앨범 등록 신청 상세 조회 성공")
        void getAlbumApplication_success() throws Exception {
            Long applicationId = 100L;

            AlbumApplicationDetailResponse response = new AlbumApplicationDetailResponse(
                    applicationId,
                    1L,
                    10L,
                    "Palette",
                    "정규 앨범",
                    "https://example.com/album.jpg",
                    0,
                    ApplicationStatus.PENDING,
                    null,
                    null,
                    null,
                    LocalDateTime.of(2026, 4, 14, 15, 0, 0),
                    LocalDateTime.of(2026, 4, 14, 15, 0, 0)
            );

            given(albumService.getAlbumApplication(any(), eq(applicationId))).willReturn(response);

            mockMvc.perform(get("/api/album-applications/{applicationId}", applicationId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("앨범 등록 신청 상세 조회 성공"))
                    .andExpect(jsonPath("$.data.applicationId").value(applicationId))
                    .andExpect(jsonPath("$.data.title").value("Palette"))
                    .andDo(document("album-applications-get",
                            pathParameters(
                                    parameterWithName("applicationId").description("신청 ID")
                            ),
                            responseFields(
                                    baseSuccessResponseFields()
                            ).and(
                                    albumApplicationDetailResponseFields()
                            )
                    ));
        }

        @Test
        @DisplayName("다른 사용자의 신청 조회 시 403 반환")
        void getAlbumApplication_fail_forbidden() throws Exception {
            Long applicationId = 100L;

            given(albumService.getAlbumApplication(any(), eq(applicationId)))
                    .willThrow(new BusinessException(
                            AlbumApplicationErrorCode.ERR_ALBUM_APPLICATION_DETAIL_FORBIDDEN
                    ));

            mockMvc.perform(get("/api/album-applications/{applicationId}", applicationId))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(
                            AlbumApplicationErrorCode.ERR_ALBUM_APPLICATION_DETAIL_FORBIDDEN.getMessage()
                    ))
                    .andDo(document("album-applications-get-forbidden",
                            pathParameters(
                                    parameterWithName("applicationId").description("신청 ID")
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            )
                    ));
        }

        @Test
        @DisplayName("존재하지 않는 신청 조회 시 404 반환")
        void getAlbumApplication_fail_notFound() throws Exception {
            Long applicationId = 999L;

            given(albumService.getAlbumApplication(any(), eq(applicationId)))
                    .willThrow(new BusinessException(
                            AlbumApplicationErrorCode.ERR_ALBUM_APPLICATION_NOT_FOUND
                    ));

            mockMvc.perform(get("/api/album-applications/{applicationId}", applicationId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(
                            AlbumApplicationErrorCode.ERR_ALBUM_APPLICATION_NOT_FOUND.getMessage()
                    ))
                    .andDo(document("album-applications-get-not-found",
                            pathParameters(
                                    parameterWithName("applicationId").description("신청 ID")
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            )
                    ));
        }
    }

    @Nested
    @WithMockUser(roles = "ADMIN")
    @DisplayName("앨범 등록 신청 목록 조회 API")
    class GetAlbumApplications {

        @Test
        @DisplayName("앨범 등록 신청 목록 조회 성공")
        void getAlbumApplications_success() throws Exception {
            AlbumApplicationListResponse content = new AlbumApplicationListResponse(
                    1L,
                    1L,
                    10L,
                    "첫 번째 신청",
                    ApplicationStatus.PENDING,
                    LocalDateTime.of(2026, 4, 14, 15, 0, 0)
            );

            PageResponse<AlbumApplicationListResponse> response = PageResponse.from(
                    new PageImpl<>(List.of(content), PageRequest.of(0, 10), 1)
            );

            given(albumService.getAlbumApplications(eq(null), any(Pageable.class)))
                    .willReturn(response);

            mockMvc.perform(get("/api/admin/album-applications")
                            .param("page", "0")
                            .param("size", "10")
                            .param("sort", "createdAt,asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("앨범 등록 신청 목록 조회 성공"))
                    .andExpect(jsonPath("$.data.content[0].applicationId").value(1L))
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(10))
                    .andExpect(jsonPath("$.data.totalPages").value(1))
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andDo(document("admin-album-applications-get",
                            queryParameters(
                                    parameterWithName("status").optional().description("신청 상태"),
                                    parameterWithName("page").optional().description("페이지 번호"),
                                    parameterWithName("size").optional().description("페이지 크기"),
                                    parameterWithName("sort").optional().description("정렬 조건")
                            ),
                            responseFields(
                                    baseSuccessResponseFields()
                            ).and(
                                    fieldWithPath("data.content").type(ARRAY).description("신청 목록"),
                                    fieldWithPath("data.content[].applicationId").type(NUMBER).description("신청 ID"),
                                    fieldWithPath("data.content[].requesterUserId").type(NUMBER).description("신청자 ID"),
                                    fieldWithPath("data.content[].artistId").type(NUMBER).description("아티스트 ID"),
                                    fieldWithPath("data.content[].title").type(STRING).description("앨범 제목"),
                                    fieldWithPath("data.content[].status").type(STRING).description("신청 상태"),
                                    fieldWithPath("data.content[].createdAt").type(STRING).description("신청 생성 시각")
                            ).and(
                                    pageResponseFields()
                            )
                    ));
        }
    }

    @Nested
    @WithMockUser(roles = "ADMIN")
    @DisplayName("앨범 등록 신청 승인 API")
    class ApproveAlbumApplication {

        @Test
        @DisplayName("앨범 등록 신청 승인 성공")
        void approveAlbumApplication_success() throws Exception {
            Long applicationId = 10L;

            AlbumApplicationApproveResponse response = new AlbumApplicationApproveResponse(
                    applicationId,
                    1000L,
                    ApplicationStatus.APPROVED,
                    1L,
                    LocalDateTime.of(2026, 4, 17, 12, 0, 0)
            );

            given(albumService.approveAlbumApplication(any(), eq(applicationId)))
                    .willReturn(response);

            mockMvc.perform(post("/api/admin/album-applications/{applicationId}/approve", applicationId)
                            .with(csrf().asHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("앨범 등록 신청 승인 성공"))
                    .andExpect(jsonPath("$.data.applicationId").value(applicationId))
                    .andExpect(jsonPath("$.data.albumId").value(1000L))
                    .andDo(document("admin-album-applications-approve",
                            pathParameters(
                                    parameterWithName("applicationId").description("신청 ID")
                            ),
                            responseFields(
                                    baseSuccessResponseFields()
                            ).and(
                                    fieldWithPath("data.applicationId").type(NUMBER).description("신청 ID"),
                                    fieldWithPath("data.albumId").type(NUMBER).description("생성된 앨범 ID"),
                                    fieldWithPath("data.status").type(STRING).description("신청 상태"),
                                    fieldWithPath("data.reviewedByAdminId").type(NUMBER).description("승인 관리자 ID"),
                                    fieldWithPath("data.reviewedAt").type(STRING).description("승인 시각")
                            )
                    ));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("앨범 등록 신청 승인 시 이미 처리된 신청이면 400 반환")
        void approveAlbumApplication_fail_whenAlreadyProcessed() throws Exception {
            Long applicationId = 10L;

            given(albumService.approveAlbumApplication(any(), eq(applicationId)))
                    .willThrow(new BusinessException(
                            AlbumApplicationErrorCode.ERR_ALBUM_APPLICATION_ALREADY_PROCESSED
                    ));

            mockMvc.perform(post("/api/admin/album-applications/{applicationId}/approve", applicationId)
                            .with(csrf().asHeader()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            AlbumApplicationErrorCode.ERR_ALBUM_APPLICATION_ALREADY_PROCESSED.getMessage()
                    ))
                    .andDo(document("admin-album-applications-approve-already-processed",
                            pathParameters(
                                    parameterWithName("applicationId").description("신청 ID")
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            )
                    ));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("앨범 등록 신청 승인 시 존재하지 않는 신청이면 404 반환")
        void approveAlbumApplication_fail_notFound() throws Exception {
            Long applicationId = 999L;

            given(albumService.approveAlbumApplication(any(), eq(applicationId)))
                    .willThrow(new BusinessException(
                            AlbumApplicationErrorCode.ERR_ALBUM_APPLICATION_NOT_FOUND
                    ));

            mockMvc.perform(post("/api/admin/album-applications/{applicationId}/approve", applicationId)
                            .with(csrf().asHeader()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(
                            AlbumApplicationErrorCode.ERR_ALBUM_APPLICATION_NOT_FOUND.getMessage()
                    ))
                    .andDo(document("admin-album-applications-approve-not-found",
                            pathParameters(
                                    parameterWithName("applicationId").description("신청 ID")
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            )
                    ));
        }
    }

    @Nested
    @WithMockUser(roles = "ADMIN")
    @DisplayName("앨범 등록 신청 거절 API")
    class RejectAlbumApplication {

        @Test
        @DisplayName("앨범 등록 신청 거절 성공")
        void rejectAlbumApplication_success() throws Exception {
            Long applicationId = 10L;
            AlbumApplicationRejectRequest request =
                    new AlbumApplicationRejectRequest("앨범 정보가 부족합니다");

            AlbumApplicationRejectResponse response = new AlbumApplicationRejectResponse(
                    applicationId,
                    ApplicationStatus.REJECTED,
                    1L,
                    LocalDateTime.of(2026, 4, 17, 12, 0, 0),
                    "앨범 정보가 부족합니다"
            );

            given(albumService.rejectAlbumApplication(any(), eq(applicationId), eq(request.rejectionReason())))
                    .willReturn(response);

            mockMvc.perform(post("/api/admin/album-applications/{applicationId}/reject", applicationId)
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("앨범 등록 신청 거절 성공"))
                    .andExpect(jsonPath("$.data.applicationId").value(applicationId))
                    .andExpect(jsonPath("$.data.rejectionReason").value("앨범 정보가 부족합니다"))
                    .andDo(document("admin-album-applications-reject",
                            pathParameters(
                                    parameterWithName("applicationId").description("신청 ID")
                            ),
                            requestFields(
                                    albumApplicationRejectRequestFields()
                            ),
                            responseFields(
                                    baseSuccessResponseFields()
                            ).and(
                                    fieldWithPath("data.applicationId").type(NUMBER).description("신청 ID"),
                                    fieldWithPath("data.status").type(STRING).description("신청 상태"),
                                    fieldWithPath("data.reviewedByAdminId").type(NUMBER).description("거절 관리자 ID"),
                                    fieldWithPath("data.reviewedAt").type(STRING).description("거절 시각"),
                                    fieldWithPath("data.rejectionReason").type(STRING).description("거절 사유")
                            )
                    ));
        }

        @Test
        @DisplayName("거절 사유 없이 요청 시 400 반환")
        void rejectAlbumApplication_fail_withoutReason() throws Exception {
            AlbumApplicationRejectRequest request =
                    new AlbumApplicationRejectRequest("");

            mockMvc.perform(post("/api/admin/album-applications/10/reject")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andDo(document("admin-album-applications-reject-invalid",
                            requestFields(
                                    fieldWithPath("rejectionReason").type(STRING).description("거절 사유 (값 없음)")
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            ).and(
                                    validationErrorResponseFields()
                            )
                    ));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("앨범 등록 신청 거절 시 이미 처리된 신청이면 400 반환")
        void rejectAlbumApplication_fail_whenAlreadyProcessed() throws Exception {
            Long applicationId = 10L;

            AlbumApplicationRejectRequest request =
                    new AlbumApplicationRejectRequest("사유");

            given(albumService.rejectAlbumApplication(any(), eq(applicationId), any()))
                    .willThrow(new BusinessException(
                            AlbumApplicationErrorCode.ERR_ALBUM_APPLICATION_ALREADY_PROCESSED
                    ));

            mockMvc.perform(post("/api/admin/album-applications/{applicationId}/reject", applicationId)
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            AlbumApplicationErrorCode.ERR_ALBUM_APPLICATION_ALREADY_PROCESSED.getMessage()
                    ))
                    .andDo(document("admin-album-applications-reject-already-processed",
                            pathParameters(
                                    parameterWithName("applicationId").description("신청 ID")
                            ),
                            requestFields(
                                    fieldWithPath("rejectionReason").description("거절 사유")
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            )
                    ));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("앨범 등록 신청 거절 시 존재하지 않는 신청이면 404 반환")
        void rejectAlbumApplication_fail_notFound() throws Exception {
            Long applicationId = 999L;

            AlbumApplicationRejectRequest request =
                    new AlbumApplicationRejectRequest("사유");

            given(albumService.rejectAlbumApplication(any(), eq(applicationId), any()))
                    .willThrow(new BusinessException(
                            AlbumApplicationErrorCode.ERR_ALBUM_APPLICATION_NOT_FOUND
                    ));

            mockMvc.perform(post("/api/admin/album-applications/{applicationId}/reject", applicationId)
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(
                            AlbumApplicationErrorCode.ERR_ALBUM_APPLICATION_NOT_FOUND.getMessage()
                    ))
                    .andDo(document("admin-album-applications-reject-not-found",
                            pathParameters(
                                    parameterWithName("applicationId").description("신청 ID")
                            ),
                            requestFields(
                                    fieldWithPath("rejectionReason").description("거절 사유")
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            )
                    ));
        }
    }

    @Nested
    @WithMockUser
    @DisplayName("앨범 상세 조회 API")
    class GetAlbum {

        @Test
        @DisplayName("앨범 상세 조회 성공")
        void getAlbum_success() throws Exception {
            Long albumId = 1L;

            AlbumDetailResponse response = new AlbumDetailResponse(
                    albumId,
                    10L,
                    "아이유",
                    "Palette",
                    "정규 앨범",
                    "https://example.com/album.jpg",
                    10L,
                    2100L,
                    LocalDateTime.of(2026, 5, 1, 18, 0, 0),
                    List.of(
                            new AlbumTrackResponse(1000L, 1L, "이름에게", 245L),
                            new AlbumTrackResponse(1001L, 2L, "밤편지", 230L)
                    )
            );

            given(albumService.getAlbum(albumId)).willReturn(response);

            mockMvc.perform(get("/api/albums/{albumId}", albumId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("앨범 상세 조회 성공"))
                    .andExpect(jsonPath("$.data.albumId").value(albumId))
                    .andExpect(jsonPath("$.data.artistName").value("아이유"))
                    .andExpect(jsonPath("$.data.tracks[0].trackId").value(1000L))
                    .andExpect(jsonPath("$.data.tracks[0].trackNumber").value(1L))
                    .andDo(document("albums-get",
                            pathParameters(
                                    parameterWithName("albumId").description("앨범 ID")
                            ),
                            responseFields(
                                    baseSuccessResponseFields()
                            ).and(
                                    albumDetailResponseFields()
                            )
                    ));
        }

        @Test
        @DisplayName("앨범 상세 조회 시 존재하지 않는 앨범이면 404 반환")
        void getAlbum_fail_notFound() throws Exception {
            Long albumId = 999L;

            given(albumService.getAlbum(albumId))
                    .willThrow(new BusinessException(
                            AlbumErrorCode.ERR_ALBUM_NOT_FOUND
                    ));

            mockMvc.perform(get("/api/albums/{albumId}", albumId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(
                            AlbumErrorCode.ERR_ALBUM_NOT_FOUND.getMessage()
                    ))
                    .andDo(document("albums-get-not-found",
                            pathParameters(
                                    parameterWithName("albumId").description("앨범 ID")
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            )
                    ));
        }
    }

    @Nested
    @WithMockUser
    @DisplayName("아티스트별 앨범 목록 조회 API")
    class GetArtistAlbums {

        @Test
        @DisplayName("아티스트별 앨범 목록 조회 성공")
        void getArtistAlbums_success() throws Exception {
            Long artistId = 10L;

            ArtistAlbumListResponse album1 = new ArtistAlbumListResponse(
                    100L,
                    "Palette",
                    "https://example.com/album1.jpg",
                    10L
            );
            ArtistAlbumListResponse album2 = new ArtistAlbumListResponse(
                    101L,
                    "Love poem",
                    "https://example.com/album2.jpg",
                    6L
            );

            List<ArtistAlbumListResponse> response = List.of(album1, album2);

            given(albumService.getArtistAlbums(artistId))
                    .willReturn(response);

            mockMvc.perform(get("/api/artists/{artistId}/albums", artistId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("아티스트별 앨범 목록 조회 성공"))
                    .andExpect(jsonPath("$.data[0].albumId").value(100L))
                    .andExpect(jsonPath("$.data[0].title").value("Palette"))
                    .andDo(document("artists-albums-get",
                            pathParameters(
                                    parameterWithName("artistId").description("아티스트 ID")
                            ),
                            responseFields(
                                    baseSuccessResponseFields()
                            ).and(
                                    fieldWithPath("data").type(ARRAY).description("앨범 목록"),
                                    fieldWithPath("data[].albumId").type(NUMBER).description("앨범 ID"),
                                    fieldWithPath("data[].title").type(STRING).description("앨범 제목"),
                                    fieldWithPath("data[].coverImageUrl").type(STRING).description("커버 이미지 URL"),
                                    fieldWithPath("data[].trackCount").type(NUMBER).description("트랙 수")
                            )
                    ));
        }

        @Test
        @DisplayName("아티스트별 앨범 목록 조회 시 존재하지 않는 아티스트이면 404 반환")
        void getAlbumsByArtist_fail_notFound() throws Exception {
            Long artistId = 999L;

            given(albumService.getArtistAlbums(artistId))
                    .willThrow(new BusinessException(
                            ArtistErrorCode.ERR_ARTIST_NOT_FOUND
                    ));

            mockMvc.perform(get("/api/artists/{artistId}/albums", artistId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(
                            ArtistErrorCode.ERR_ARTIST_NOT_FOUND.getMessage()
                    ))
                    .andDo(document("albums-by-artist-not-found",
                            pathParameters(
                                    parameterWithName("artistId").description("아티스트 ID")
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

    private FieldDescriptor[] albumApplicationResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("data.applicationId").type(NUMBER).description("신청 ID"),
                fieldWithPath("data.artistId").type(NUMBER).description("아티스트 ID"),
                fieldWithPath("data.title").type(STRING).description("앨범 제목"),
                fieldWithPath("data.status").type(STRING).description("신청 상태"),
                fieldWithPath("data.createdAt").type(STRING).description("신청 생성 시각")
        };
    }

    private FieldDescriptor[] albumApplicationDetailResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("data.applicationId").type(NUMBER).description("신청 ID"),
                fieldWithPath("data.requesterUserId").type(NUMBER).description("신청자 ID"),
                fieldWithPath("data.artistId").type(NUMBER).description("아티스트 ID"),
                fieldWithPath("data.title").type(STRING).description("앨범 제목"),
                fieldWithPath("data.description").type(STRING).description("앨범 설명"),
                fieldWithPath("data.coverImageUrl").type(STRING).description("커버 이미지 URL"),
                fieldWithPath("data.publishDelayDays").type(NUMBER).description("공개 예약 일수"),
                fieldWithPath("data.status").type(STRING).description("신청 상태"),
                fieldWithPath("data.reviewedByAdminId").type(NULL).description("검토 관리자 ID"),
                fieldWithPath("data.reviewedAt").type(NULL).description("검토 시각"),
                fieldWithPath("data.rejectionReason").type(NULL).description("거절 사유"),
                fieldWithPath("data.createdAt").type(STRING).description("신청 생성 시각"),
                fieldWithPath("data.updatedAt").type(STRING).description("신청 수정 시각")
        };
    }

    private FieldDescriptor[] albumDetailResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("data.albumId").type(NUMBER).description("앨범 ID"),
                fieldWithPath("data.artistId").type(NUMBER).description("아티스트 ID"),
                fieldWithPath("data.artistName").type(STRING).description("아티스트 이름"),
                fieldWithPath("data.title").type(STRING).description("앨범 제목"),
                fieldWithPath("data.description").type(STRING).description("앨범 설명"),
                fieldWithPath("data.coverImageUrl").type(STRING).description("커버 이미지 URL"),
                fieldWithPath("data.trackCount").type(NUMBER).description("트랙 수"),
                fieldWithPath("data.totalDurationSec").type(NUMBER).description("총 재생 시간(초)"),
                fieldWithPath("data.publishedAt").type(STRING).description("공개 시각"),
                fieldWithPath("data.tracks").type(ARRAY).description("수록곡 목록"),
                fieldWithPath("data.tracks[].trackId").type(NUMBER).description("트랙 ID"),
                fieldWithPath("data.tracks[].trackNumber").type(NUMBER).description("트랙 번호"),
                fieldWithPath("data.tracks[].title").type(STRING).description("트랙 제목"),
                fieldWithPath("data.tracks[].durationSec").type(NUMBER).description("재생 시간(초)")
        };
    }

    private FieldDescriptor[] albumApplicationCreateRequestFields() {
        return new FieldDescriptor[]{
                fieldWithPath("artistId").type(NUMBER).description("아티스트 ID"),
                fieldWithPath("title").type(STRING).description("앨범 제목"),
                fieldWithPath("description").type(STRING).description("앨범 설명"),
                fieldWithPath("coverImageUrl").type(STRING).description("커버 이미지 URL"),
                fieldWithPath("publishDelayDays").type(NUMBER).description("공개 예약 일수 (0~7)")
        };
    }

    private FieldDescriptor[] albumApplicationRejectRequestFields() {
        return new FieldDescriptor[]{
                fieldWithPath("rejectionReason").type(STRING).description("거절 사유")
        };
    }
}
