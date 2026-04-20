package com.fivefy.domain.track.controller;

import com.fivefy.common.config.security.JwtUtil;
import com.fivefy.common.docs.RestDocsSupport;
import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.common.filter.LastActiveAtFilter;
import com.fivefy.domain.track.dto.request.FreeTrackApplicationCreateRequest;
import com.fivefy.domain.track.dto.request.OfficialTrackApplicationCreateRequest;
import com.fivefy.domain.track.dto.request.TrackApplicationRejectRequest;
import com.fivefy.domain.track.dto.response.*;
import com.fivefy.domain.track.enums.TrackApplicationErrorCode;
import com.fivefy.domain.track.enums.TrackType;
import com.fivefy.domain.track.service.TrackService;
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
 * TrackApplicationController 웹 계층 테스트 및 REST Docs 문서화
 */
@WebMvcTest(TrackApplicationController.class)
class TrackApplicationControllerTest extends RestDocsSupport {

    @MockitoBean
    private TrackService trackService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private LastActiveAtFilter lastActiveAtFilter;

    @Nested
    @WithMockUser
    @DisplayName("자유 창작 트랙 등록 신청 API")
    class CreateFreeTrackApplication {

        @Test
        @DisplayName("자유 창작 트랙 등록 신청 성공")
        void createFreeTrackApplication_success() throws Exception {
            FreeTrackApplicationCreateRequest request = new FreeTrackApplicationCreateRequest(
                    "밤편지 AI 버전",
                    "가사",
                    "BALLAD",
                    "https://example.com/audio.mp3",
                    210L
            );

            TrackApplicationResponse response = new TrackApplicationResponse(
                    1L,
                    TrackType.FREE_CREATION,
                    null,
                    null,
                    "밤편지 AI 버전",
                    ApplicationStatus.PENDING,
                    LocalDateTime.of(2026, 4, 19, 18, 0, 0)
            );

            given(trackService.createFreeTrackApplication(any(), any(FreeTrackApplicationCreateRequest.class)))
                    .willReturn(response);

            mockMvc.perform(post("/api/track-applications/free-creations")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("자유 창작 트랙 등록 신청 성공"))
                    .andExpect(jsonPath("$.data.applicationId").value(1L))
                    .andDo(document("track-applications-free-creations-create",
                            requestFields(
                                    freeTrackApplicationCreateRequestFields()
                            ),
                            responseFields(
                                    baseSuccessResponseFields()
                            ).and(
                                    trackApplicationResponseFieldsForFreeCreation()
                            )
                    ));
        }

        @Test
        @DisplayName("제목 없이 생성 요청 시 400 반환")
        void createFreeTrackApplication_fail_withoutTitle() throws Exception {
            FreeTrackApplicationCreateRequest request = new FreeTrackApplicationCreateRequest(
                    "",
                    "가사",
                    "BALLAD",
                    "https://example.com/audio.mp3",
                    210L
            );

            mockMvc.perform(post("/api/track-applications/free-creations")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andDo(document("track-applications-free-creations-create-invalid",
                            requestFields(
                                    fieldWithPath("title").type(STRING).description("트랙 제목 (값 없음)"),
                                    fieldWithPath("lyrics").type(STRING).description("가사"),
                                    fieldWithPath("genre").type(STRING).description("장르"),
                                    fieldWithPath("audioUrl").type(STRING).description("오디오 URL"),
                                    fieldWithPath("durationSec").type(NUMBER).description("재생 시간(초)")
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            ).and(
                                    validationErrorResponseFields()
                            )
                    ));
        }

        @Test
        @DisplayName("중복 신청 시 409 반환")
        void createFreeTrackApplication_fail_whenAlreadyExists() throws Exception {
            FreeTrackApplicationCreateRequest request = new FreeTrackApplicationCreateRequest(
                    "밤편지 AI 버전",
                    "가사",
                    "BALLAD",
                    "https://example.com/audio.mp3",
                    210L
            );

            given(trackService.createFreeTrackApplication(any(), any(FreeTrackApplicationCreateRequest.class)))
                    .willThrow(new BusinessException(
                            TrackApplicationErrorCode.ERR_TRACK_APPLICATION_ALREADY_EXISTS
                    ));

            mockMvc.perform(post("/api/track-applications/free-creations")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(
                            TrackApplicationErrorCode.ERR_TRACK_APPLICATION_ALREADY_EXISTS.getMessage()
                    ))
                    .andDo(document("track-applications-free-creations-create-duplicate",
                            requestFields(
                                    freeTrackApplicationCreateRequestFields()
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            )
                    ));
        }
    }



    @Nested
    @WithMockUser
    @DisplayName("정식 발매 트랙 등록 신청 API")
    class CreateOfficialTrackApplication {

        @Test
        @DisplayName("정식 발매 트랙 등록 신청 성공")
        void createOfficialTrackApplication_success() throws Exception {
            OfficialTrackApplicationCreateRequest request = new OfficialTrackApplicationCreateRequest(
                    10L,
                    100L,
                    1L,
                    "밤편지",
                    "가사",
                    "BALLAD",
                    "https://example.com/audio.mp3",
                    230L,
                    "feat. 10cm",
                    3
            );

            TrackApplicationResponse response = new TrackApplicationResponse(
                    1L,
                    TrackType.OFFICIAL_RELEASE,
                    10L,
                    100L,
                    "밤편지",
                    ApplicationStatus.PENDING,
                    LocalDateTime.of(2026, 4, 19, 18, 0, 0)
            );

            given(trackService.createOfficialTrackApplication(any(), any(OfficialTrackApplicationCreateRequest.class)))
                    .willReturn(response);

            mockMvc.perform(post("/api/track-applications/official-releases")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("정식 발매 트랙 등록 신청 성공"))
                    .andExpect(jsonPath("$.data.applicationId").value(1L))
                    .andExpect(jsonPath("$.data.trackType").value("OFFICIAL_RELEASE"))
                    .andDo(document("track-applications-official-releases-create",
                            requestFields(
                                    officialTrackApplicationCreateRequestFields()
                            ),
                            responseFields(
                                    baseSuccessResponseFields()
                            ).and(
                                    trackApplicationResponseFieldsForOfficialRelease()
                            )
                    ));
        }

        @Test
        @DisplayName("제목 없이 생성 요청 시 400 반환")
        void createOfficialTrackApplication_fail_withoutTitle() throws Exception {
            OfficialTrackApplicationCreateRequest request = new OfficialTrackApplicationCreateRequest(
                    10L,
                    100L,
                    1L,
                    "",
                    "가사",
                    "BALLAD",
                    "https://example.com/audio.mp3",
                    230L,
                    "feat. 10cm",
                    3
            );

            mockMvc.perform(post("/api/track-applications/official-releases")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andDo(document("track-applications-official-releases-create-invalid",
                            requestFields(
                                    fieldWithPath("artistId").type(NUMBER).description("아티스트 ID"),
                                    fieldWithPath("albumId").type(NUMBER).description("앨범 ID"),
                                    fieldWithPath("trackNumber").type(NUMBER).description("트랙 번호"),
                                    fieldWithPath("title").type(STRING).description("트랙 제목 (값 없음)"),
                                    fieldWithPath("lyrics").type(STRING).description("가사"),
                                    fieldWithPath("genre").type(STRING).description("장르"),
                                    fieldWithPath("audioUrl").type(STRING).description("오디오 URL"),
                                    fieldWithPath("durationSec").type(NUMBER).description("재생 시간(초)"),
                                    fieldWithPath("featuredArtistText").type(STRING).description("피처링 아티스트 정보"),
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
        @DisplayName("중복 신청 시 409 반환")
        void createOfficialTrackApplication_fail_whenAlreadyExists() throws Exception {
            OfficialTrackApplicationCreateRequest request = new OfficialTrackApplicationCreateRequest(
                    10L,
                    100L,
                    1L,
                    "밤편지",
                    "가사",
                    "BALLAD",
                    "https://example.com/audio.mp3",
                    230L,
                    "feat. 10cm",
                    3
            );

            given(trackService.createOfficialTrackApplication(any(), any(OfficialTrackApplicationCreateRequest.class)))
                    .willThrow(new BusinessException(
                            TrackApplicationErrorCode.ERR_TRACK_APPLICATION_ALREADY_EXISTS
                    ));

            mockMvc.perform(post("/api/track-applications/official-releases")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(
                            TrackApplicationErrorCode.ERR_TRACK_APPLICATION_ALREADY_EXISTS.getMessage()
                    ))
                    .andDo(document("track-applications-official-releases-create-duplicate",
                            requestFields(
                                    officialTrackApplicationCreateRequestFields()
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            )
                    ));
        }
    }

    @Nested
    @WithMockUser
    @DisplayName("내 트랙 등록 신청 목록 조회 API")
    class GetMyTrackApplications {

        @Test
        @DisplayName("내 트랙 등록 신청 목록 조회 성공")
        void getMyTrackApplications_success() throws Exception {
            List<TrackApplicationResponse> response = List.of(
                    new TrackApplicationResponse(
                            2L,
                            TrackType.OFFICIAL_RELEASE,
                            10L,
                            100L,
                            "두 번째 신청",
                            ApplicationStatus.PENDING,
                            LocalDateTime.of(2026, 4, 16, 16, 0, 0)
                    ),
                    new TrackApplicationResponse(
                            1L,
                            TrackType.FREE_CREATION,
                            null,
                            null,
                            "첫 번째 신청",
                            ApplicationStatus.PENDING,
                            LocalDateTime.of(2026, 4, 15, 16, 0, 0)
                    )
            );

            given(trackService.getMyTrackApplications(any())).willReturn(response);

            mockMvc.perform(get("/api/track-applications/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("내 트랙 등록 신청 목록 조회 성공"))
                    .andExpect(jsonPath("$.data[0].applicationId").value(2L))
                    .andExpect(jsonPath("$.data[0].title").value("두 번째 신청"))
                    .andDo(document("track-applications-me-get",
                            responseFields(
                                    baseSuccessResponseFields()
                            ).and(
                                    fieldWithPath("data").type(ARRAY).description("내 트랙 등록 신청 목록"),
                                    fieldWithPath("data[].applicationId").type(NUMBER).description("신청 ID"),
                                    fieldWithPath("data[].trackType").type(STRING).description("트랙 유형"),
                                    fieldWithPath("data[].artistId").type(VARIES).optional().description("아티스트 ID"),
                                    fieldWithPath("data[].albumId").type(VARIES).optional().description("앨범 ID"),
                                    fieldWithPath("data[].title").type(STRING).description("트랙 제목"),
                                    fieldWithPath("data[].status").type(STRING).description("신청 상태"),
                                    fieldWithPath("data[].createdAt").type(STRING).description("신청 생성 시각")
                            )
                    ));
        }
    }

    @Nested
    @WithMockUser
    @DisplayName("트랙 등록 신청 상세 조회 API")
    class GetTrackApplication {

        @Test
        @DisplayName("트랙 등록 신청 상세 조회 성공")
        void getTrackApplication_success() throws Exception {
            Long applicationId = 100L;

            TrackApplicationDetailResponse response = new TrackApplicationDetailResponse(
                    applicationId,
                    1L,
                    TrackType.OFFICIAL_RELEASE,
                    10L,
                    100L,
                    1L,
                    "밤편지",
                    "가사",
                    "BALLAD",
                    "https://example.com/audio.mp3",
                    230L,
                    "feat. 10cm",
                    3,
                    ApplicationStatus.PENDING,
                    null,
                    null,
                    null,
                    LocalDateTime.of(2026, 4, 19, 18, 0, 0),
                    LocalDateTime.of(2026, 4, 19, 18, 0, 0)
            );

            given(trackService.getTrackApplication(any(), eq(applicationId)))
                    .willReturn(response);

            mockMvc.perform(get("/api/track-applications/{applicationId}", applicationId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("트랙 등록 신청 상세 조회 성공"))
                    .andExpect(jsonPath("$.data.applicationId").value(applicationId))
                    .andExpect(jsonPath("$.data.title").value("밤편지"))
                    .andDo(document("track-applications-get",
                            pathParameters(
                                    parameterWithName("applicationId").description("신청 ID")
                            ),
                            responseFields(
                                    baseSuccessResponseFields()
                            ).and(
                                    fieldWithPath("data.applicationId").type(NUMBER).description("신청 ID"),
                                    fieldWithPath("data.requesterUserId").type(NUMBER).description("신청자 ID"),
                                    fieldWithPath("data.trackType").type(STRING).description("트랙 유형"),
                                    fieldWithPath("data.artistId").type(NUMBER).description("아티스트 ID"),
                                    fieldWithPath("data.albumId").type(NUMBER).description("앨범 ID"),
                                    fieldWithPath("data.trackNumber").type(NUMBER).description("트랙 번호"),
                                    fieldWithPath("data.title").type(STRING).description("트랙 제목"),
                                    fieldWithPath("data.lyrics").type(STRING).description("가사"),
                                    fieldWithPath("data.genre").type(STRING).description("장르"),
                                    fieldWithPath("data.audioUrl").type(STRING).description("오디오 URL"),
                                    fieldWithPath("data.durationSec").type(NUMBER).description("재생 시간(초)"),
                                    fieldWithPath("data.featuredArtistText").type(STRING).description("피처링 아티스트 정보"),
                                    fieldWithPath("data.publishDelayDays").type(NUMBER).description("공개 예약 일수"),
                                    fieldWithPath("data.status").type(STRING).description("신청 상태"),
                                    fieldWithPath("data.reviewedByAdminId").type(NULL).description("검토 관리자 ID"),
                                    fieldWithPath("data.reviewedAt").type(NULL).description("검토 시각"),
                                    fieldWithPath("data.rejectionReason").type(NULL).description("거절 사유"),
                                    fieldWithPath("data.createdAt").type(STRING).description("신청 생성 시각"),
                                    fieldWithPath("data.updatedAt").type(STRING).description("신청 수정 시각")
                            )
                    ));
        }

        @Test
        @DisplayName("다른 사용자의 신청 조회 시 403 반환")
        void getTrackApplication_fail_forbidden() throws Exception {
            Long applicationId = 100L;

            given(trackService.getTrackApplication(any(), eq(applicationId)))
                    .willThrow(new BusinessException(
                            TrackApplicationErrorCode.ERR_TRACK_APPLICATION_DETAIL_FORBIDDEN
                    ));

            mockMvc.perform(get("/api/track-applications/{applicationId}", applicationId))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(
                            TrackApplicationErrorCode.ERR_TRACK_APPLICATION_DETAIL_FORBIDDEN.getMessage()
                    ))
                    .andDo(document("track-applications-get-forbidden",
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
        void getTrackApplication_fail_notFound() throws Exception {
            Long applicationId = 999L;

            given(trackService.getTrackApplication(any(), eq(applicationId)))
                    .willThrow(new BusinessException(
                            TrackApplicationErrorCode.ERR_TRACK_APPLICATION_NOT_FOUND
                    ));

            mockMvc.perform(get("/api/track-applications/{applicationId}", applicationId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(
                            TrackApplicationErrorCode.ERR_TRACK_APPLICATION_NOT_FOUND.getMessage()
                    ))
                    .andDo(document("track-applications-get-not-found",
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
    @DisplayName("트랙 등록 신청 목록 조회 API")
    class GetTrackApplications {

        @Test
        @DisplayName("트랙 등록 신청 목록 조회 성공")
        void getTrackApplications_success() throws Exception {
            TrackApplicationListResponse content = new TrackApplicationListResponse(
                    1L,
                    1L,
                    TrackType.OFFICIAL_RELEASE,
                    "첫 번째 신청",
                    ApplicationStatus.PENDING,
                    LocalDateTime.of(2026, 4, 19, 18, 0, 0)
            );

            PageResponse<TrackApplicationListResponse> response = PageResponse.from(
                    new PageImpl<>(List.of(content), PageRequest.of(0, 10), 1)
            );

            given(trackService.getTrackApplications(eq(null), any(Pageable.class)))
                    .willReturn(response);

            mockMvc.perform(get("/api/admin/track-applications")
                            .param("page", "0")
                            .param("size", "10")
                            .param("sort", "createdAt,asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("트랙 등록 신청 목록 조회 성공"))
                    .andExpect(jsonPath("$.data.content[0].applicationId").value(1L))
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(10))
                    .andExpect(jsonPath("$.data.totalPages").value(1))
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andDo(document("admin-track-applications-get",
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
                                    fieldWithPath("data.content[].trackType").type(STRING).description("트랙 유형"),
                                    fieldWithPath("data.content[].title").type(STRING).description("트랙 제목"),
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
    @DisplayName("트랙 등록 신청 승인 API")
    class ApproveTrackApplication {

        @Test
        @DisplayName("트랙 등록 신청 승인 성공")
        void approveTrackApplication_success() throws Exception {
            Long applicationId = 10L;

            TrackApplicationApproveResponse response = new TrackApplicationApproveResponse(
                    applicationId,
                    1000L,
                    ApplicationStatus.APPROVED,
                    1L,
                    LocalDateTime.of(2026, 4, 20, 12, 0, 0)
            );

            given(trackService.approveTrackApplication(any(), eq(applicationId)))
                    .willReturn(response);

            mockMvc.perform(post("/api/admin/track-applications/{applicationId}/approve", applicationId)
                            .with(csrf().asHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("트랙 등록 신청 승인 성공"))
                    .andExpect(jsonPath("$.data.applicationId").value(applicationId))
                    .andExpect(jsonPath("$.data.trackId").value(1000L))
                    .andDo(document("admin-track-applications-approve",
                            pathParameters(
                                    parameterWithName("applicationId").description("신청 ID")
                            ),
                            responseFields(
                                    baseSuccessResponseFields()
                            ).and(
                                    fieldWithPath("data.applicationId").type(NUMBER).description("신청 ID"),
                                    fieldWithPath("data.trackId").type(NUMBER).description("생성된 트랙 ID"),
                                    fieldWithPath("data.status").type(STRING).description("신청 상태"),
                                    fieldWithPath("data.reviewedByAdminId").type(NUMBER).description("승인 관리자 ID"),
                                    fieldWithPath("data.reviewedAt").type(STRING).description("승인 시각")
                            )
                    ));
        }

        @Test
        @DisplayName("이미 처리된 신청 승인 시 400 반환")
        void approveTrackApplication_fail_whenAlreadyProcessed() throws Exception {
            Long applicationId = 10L;

            given(trackService.approveTrackApplication(any(), eq(applicationId)))
                    .willThrow(new BusinessException(
                            TrackApplicationErrorCode.ERR_TRACK_APPLICATION_ALREADY_PROCESSED
                    ));

            mockMvc.perform(post("/api/admin/track-applications/{applicationId}/approve", applicationId)
                            .with(csrf().asHeader()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            TrackApplicationErrorCode.ERR_TRACK_APPLICATION_ALREADY_PROCESSED.getMessage()
                    ))
                    .andDo(document("admin-track-applications-approve-already-processed",
                            pathParameters(
                                    parameterWithName("applicationId").description("신청 ID")
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            )
                    ));
        }

        @Test
        @DisplayName("존재하지 않는 신청 승인 시 404 반환")
        void approveTrackApplication_fail_notFound() throws Exception {
            Long applicationId = 999L;

            given(trackService.approveTrackApplication(any(), eq(applicationId)))
                    .willThrow(new BusinessException(
                            TrackApplicationErrorCode.ERR_TRACK_APPLICATION_NOT_FOUND
                    ));

            mockMvc.perform(post("/api/admin/track-applications/{applicationId}/approve", applicationId)
                            .with(csrf().asHeader()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(
                            TrackApplicationErrorCode.ERR_TRACK_APPLICATION_NOT_FOUND.getMessage()
                    ))
                    .andDo(document("admin-track-applications-approve-not-found",
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
    @DisplayName("트랙 등록 신청 거절 API")
    class RejectTrackApplication {

        @Test
        @DisplayName("트랙 등록 신청 거절 성공")
        void rejectTrackApplication_success() throws Exception {
            Long applicationId = 10L;
            TrackApplicationRejectRequest request =
                    new TrackApplicationRejectRequest("오디오 정보가 부족합니다");

            TrackApplicationRejectResponse response = new TrackApplicationRejectResponse(
                    applicationId,
                    ApplicationStatus.REJECTED,
                    1L,
                    LocalDateTime.of(2026, 4, 20, 12, 0, 0),
                    "오디오 정보가 부족합니다"
            );

            given(trackService.rejectTrackApplication(any(), eq(applicationId), eq(request.rejectionReason())))
                    .willReturn(response);

            mockMvc.perform(post("/api/admin/track-applications/{applicationId}/reject", applicationId)
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("트랙 등록 신청 거절 성공"))
                    .andExpect(jsonPath("$.data.applicationId").value(applicationId))
                    .andExpect(jsonPath("$.data.rejectionReason").value("오디오 정보가 부족합니다"))
                    .andDo(document("admin-track-applications-reject",
                            pathParameters(
                                    parameterWithName("applicationId").description("신청 ID")
                            ),
                            requestFields(
                                    fieldWithPath("rejectionReason").type(STRING).description("거절 사유")
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
        void rejectTrackApplication_fail_withoutReason() throws Exception {
            TrackApplicationRejectRequest request =
                    new TrackApplicationRejectRequest("");

            mockMvc.perform(post("/api/admin/track-applications/{applicationId}/reject", 10L)
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andDo(document("admin-track-applications-reject-invalid",
                            pathParameters(
                                    parameterWithName("applicationId").description("신청 ID")
                            ),
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
        @DisplayName("이미 처리된 신청 거절 시 400 반환")
        void rejectTrackApplication_fail_whenAlreadyProcessed() throws Exception {
            Long applicationId = 10L;
            TrackApplicationRejectRequest request =
                    new TrackApplicationRejectRequest("사유");

            given(trackService.rejectTrackApplication(any(), eq(applicationId), any()))
                    .willThrow(new BusinessException(
                            TrackApplicationErrorCode.ERR_TRACK_APPLICATION_ALREADY_PROCESSED
                    ));

            mockMvc.perform(post("/api/admin/track-applications/{applicationId}/reject", applicationId)
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            TrackApplicationErrorCode.ERR_TRACK_APPLICATION_ALREADY_PROCESSED.getMessage()
                    ))
                    .andDo(document("admin-track-applications-reject-already-processed",
                            pathParameters(
                                    parameterWithName("applicationId").description("신청 ID")
                            ),
                            requestFields(
                                    fieldWithPath("rejectionReason").type(STRING).description("거절 사유")
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            )
                    ));
        }

        @Test
        @DisplayName("존재하지 않는 신청 거절 시 404 반환")
        void rejectTrackApplication_fail_notFound() throws Exception {
            Long applicationId = 999L;
            TrackApplicationRejectRequest request =
                    new TrackApplicationRejectRequest("사유");

            given(trackService.rejectTrackApplication(any(), eq(applicationId), any()))
                    .willThrow(new BusinessException(
                            TrackApplicationErrorCode.ERR_TRACK_APPLICATION_NOT_FOUND
                    ));

            mockMvc.perform(post("/api/admin/track-applications/{applicationId}/reject", applicationId)
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(
                            TrackApplicationErrorCode.ERR_TRACK_APPLICATION_NOT_FOUND.getMessage()
                    ))
                    .andDo(document("admin-track-applications-reject-not-found",
                            pathParameters(
                                    parameterWithName("applicationId").description("신청 ID")
                            ),
                            requestFields(
                                    fieldWithPath("rejectionReason").type(STRING).description("거절 사유")
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

    private FieldDescriptor[] freeTrackApplicationCreateRequestFields() {
        return new FieldDescriptor[]{
                fieldWithPath("title").type(STRING).description("트랙 제목"),
                fieldWithPath("lyrics").type(STRING).description("가사"),
                fieldWithPath("genre").type(STRING).description("장르"),
                fieldWithPath("audioUrl").type(STRING).description("오디오 URL"),
                fieldWithPath("durationSec").type(NUMBER).description("재생 시간(초)")
        };
    }

    private FieldDescriptor[] officialTrackApplicationCreateRequestFields() {
        return new FieldDescriptor[]{
                fieldWithPath("artistId").type(NUMBER).description("아티스트 ID"),
                fieldWithPath("albumId").type(NUMBER).description("앨범 ID"),
                fieldWithPath("trackNumber").type(NUMBER).description("트랙 번호"),
                fieldWithPath("title").type(STRING).description("트랙 제목"),
                fieldWithPath("lyrics").type(STRING).description("가사"),
                fieldWithPath("genre").type(STRING).description("장르"),
                fieldWithPath("audioUrl").type(STRING).description("오디오 URL"),
                fieldWithPath("durationSec").type(NUMBER).description("재생 시간(초)"),
                fieldWithPath("featuredArtistText").type(STRING).description("피처링 아티스트 정보"),
                fieldWithPath("publishDelayDays").type(NUMBER).description("공개 예약 일수 (0~7)")
        };
    }

    private FieldDescriptor[] trackApplicationResponseFieldsForFreeCreation() {
        return new FieldDescriptor[]{
                fieldWithPath("data.applicationId").type(NUMBER).description("신청 ID"),
                fieldWithPath("data.trackType").type(STRING).description("트랙 유형"),
                fieldWithPath("data.artistId").type(NULL).optional().description("아티스트 ID (자유 창작은 없음)"),
                fieldWithPath("data.albumId").type(NULL).optional().description("앨범 ID (자유 창작은 없음)"),
                fieldWithPath("data.title").type(STRING).description("트랙 제목"),
                fieldWithPath("data.status").type(STRING).description("신청 상태"),
                fieldWithPath("data.createdAt").type(STRING).description("신청 생성 시각")
        };
    }

    private FieldDescriptor[] trackApplicationResponseFieldsForOfficialRelease() {
        return new FieldDescriptor[]{
                fieldWithPath("data.applicationId").type(NUMBER).description("신청 ID"),
                fieldWithPath("data.trackType").type(STRING).description("트랙 유형"),
                fieldWithPath("data.artistId").type(NUMBER).description("아티스트 ID"),
                fieldWithPath("data.albumId").type(NUMBER).description("앨범 ID"),
                fieldWithPath("data.title").type(STRING).description("트랙 제목"),
                fieldWithPath("data.status").type(STRING).description("신청 상태"),
                fieldWithPath("data.createdAt").type(STRING).description("신청 생성 시각")
        };
    }
}