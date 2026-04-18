package com.fivefy.domain.artist.controller;

import com.fivefy.common.config.security.JwtUtil;
import com.fivefy.common.docs.RestDocsSupport;
import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.common.filter.LastActiveAtFilter;
import com.fivefy.domain.artist.dto.request.ArtistApplicationCreateRequest;
import com.fivefy.domain.artist.dto.request.ArtistApplicationRejectRequest;
import com.fivefy.domain.artist.dto.request.ArtistProfileUpdateRequest;
import com.fivefy.domain.artist.dto.response.*;
import com.fivefy.domain.artist.enums.ArtistApplicationErrorCode;
import com.fivefy.domain.artist.enums.ArtistErrorCode;
import com.fivefy.domain.artist.enums.ArtistStatus;
import com.fivefy.domain.artist.enums.ArtistType;
import com.fivefy.domain.artist.service.ArtistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ArtistController 웹 계층 테스트 및 REST Docs 문서화
 */
@WithMockUser
@WebMvcTest(ArtistController.class)
class ArtistControllerTest extends RestDocsSupport {

    @MockitoBean
    private ArtistService artistService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private LastActiveAtFilter lastActiveAtFilter;

    @Nested
    @DisplayName("아티스트 등록 신청 생성 API")
    class CreateArtistApplication {

        @Test
        @DisplayName("아티스트 등록 신청 생성 성공 시 201 반환")
        void createArtistApplication_success() throws Exception {
            ArtistApplicationCreateRequest request = new ArtistApplicationCreateRequest(
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/profile.jpg"
            );

            ArtistApplicationResponse response = new ArtistApplicationResponse(
                    1L,
                    "아이유",
                    ArtistType.SOLO.name(),
                    ApplicationStatus.PENDING.name(),
                    LocalDateTime.of(2026, 4, 18, 12, 0, 0)
            );

            given(artistService.createArtistApplication(any(), any(ArtistApplicationCreateRequest.class)))
                    .willReturn(response);

            mockMvc.perform(post("/api/artist-applications")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("아티스트 등록 신청 성공"))
                    .andExpect(jsonPath("$.data.applicationId").value(1L))
                    .andExpect(jsonPath("$.data.requestedName").value("아이유"))
                    .andExpect(jsonPath("$.data.artistType").value("SOLO"))
                    .andDo(document("artist-applications-create",
                            requestFields(
                                    fieldWithPath("requestedName").type(STRING).description("아티스트 이름"),
                                    fieldWithPath("artistType").type(STRING).description("아티스트 유형"),
                                    fieldWithPath("bio").type(STRING).description("아티스트 소개"),
                                    fieldWithPath("profileImageUrl").type(STRING).description("프로필 이미지 URL")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지"),
                                    fieldWithPath("data.applicationId").type(NUMBER).description("신청 ID"),
                                    fieldWithPath("data.requestedName").type(STRING).description("아티스트 이름"),
                                    fieldWithPath("data.artistType").type(STRING).description("아티스트 유형"),
                                    fieldWithPath("data.status").type(STRING).description("신청 상태"),
                                    fieldWithPath("data.createdAt").type(STRING).description("신청 생성 시각")
                            )
                    ));
        }

        @Test
        @DisplayName("아티스트 등록 신청 생성 시 요청 검증 실패하면 400 반환")
        void createArtistApplication_fail_withoutRequestedName() throws Exception {
            ArtistApplicationCreateRequest request = new ArtistApplicationCreateRequest(
                    "",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/profile.jpg"
            );

            mockMvc.perform(post("/api/artist-applications")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andDo(document("artist-applications-create-invalid",
                            requestFields(
                                    fieldWithPath("requestedName").type(STRING).description("아티스트 이름 (값 없음)"),
                                    fieldWithPath("artistType").type(STRING).description("아티스트 유형"),
                                    fieldWithPath("bio").type(STRING).description("아티스트 소개"),
                                    fieldWithPath("profileImageUrl").type(STRING).description("프로필 이미지 URL")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("에러 메시지"),
                                    fieldWithPath("data").type(ARRAY).description("상세 에러 내역"),
                                    fieldWithPath("data[].field").type(STRING).description("에러가 발생한 필드명"),
                                    fieldWithPath("data[].message").type(STRING).description("에러 상세 사유")
                            )
                    ));
        }

        @Test
        @DisplayName("중복 신청이면 409 반환")
        void createArtistApplication_fail_whenAlreadyExists() throws Exception {
            ArtistApplicationCreateRequest request = new ArtistApplicationCreateRequest(
                    "아이유",
                    ArtistType.SOLO,
                    "가수",
                    "https://example.com/profile.jpg"
            );

            given(artistService.createArtistApplication(any(), any(ArtistApplicationCreateRequest.class)))
                    .willThrow(new BusinessException(
                            ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_ALREADY_EXISTS
                    ));

            mockMvc.perform(post("/api/artist-applications")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(
                            ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_ALREADY_EXISTS.getMessage()
                    ))
                    .andDo(document("artist-applications-create-duplicate",
                            requestFields(
                                    fieldWithPath("requestedName").type(STRING).description("아티스트 이름"),
                                    fieldWithPath("artistType").type(STRING).description("아티스트 유형"),
                                    fieldWithPath("bio").type(STRING).description("아티스트 소개"),
                                    fieldWithPath("profileImageUrl").type(STRING).description("프로필 이미지 URL")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("에러 메시지")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("내 아티스트 신청 목록 조회 API")
    class GetMyArtistApplications {

        @Test
        @DisplayName("내 아티스트 신청 목록 조회 성공")
        void getMyArtistApplications_success() throws Exception {
            List<ArtistApplicationResponse> response = List.of(
                    new ArtistApplicationResponse(
                            2L,
                            "아이유",
                            ArtistType.SOLO.name(),
                            ApplicationStatus.PENDING.name(),
                            LocalDateTime.of(2026, 4, 18, 12, 0, 0)
                    ),
                    new ArtistApplicationResponse(
                            1L,
                            "아이유 밴드",
                            ArtistType.COLLABORATION.name(),
                            ApplicationStatus.PENDING.name(),
                            LocalDateTime.of(2026, 4, 17, 12, 0, 0)
                    )
            );

            given(artistService.getMyArtistApplications(any()))
                    .willReturn(response);

            mockMvc.perform(get("/api/artist-applications/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].applicationId").value(2L))
                    .andDo(document("artist-applications-me",
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지"),
                                    fieldWithPath("data").type(ARRAY).description("내 신청 목록"),
                                    fieldWithPath("data[].applicationId").type(NUMBER).description("신청 ID"),
                                    fieldWithPath("data[].requestedName").type(STRING).description("아티스트 이름"),
                                    fieldWithPath("data[].artistType").type(STRING).description("아티스트 유형"),
                                    fieldWithPath("data[].status").type(STRING).description("신청 상태"),
                                    fieldWithPath("data[].createdAt").type(STRING).description("생성 시각")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("아티스트 신청 상세 조회 API")
    class GetArtistApplication {

        @Test
        @DisplayName("아티스트 신청 상세 조회 성공")
        void getArtistApplication_success() throws Exception {
            Long applicationId = 10L;

            ArtistApplicationDetailResponse response = new ArtistApplicationDetailResponse(
                    applicationId,
                    1L,
                    "아이유",
                    ArtistType.SOLO.name(),
                    "가수",
                    "https://example.com/iu.jpg",
                    ApplicationStatus.PENDING.name(),
                    null,
                    null,
                    null,
                    LocalDateTime.of(2026, 4, 18, 12, 0, 0),
                    LocalDateTime.of(2026, 4, 18, 12, 0, 0)
            );

            given(artistService.getArtistApplication(any(), eq(applicationId)))
                    .willReturn(response);

            mockMvc.perform(get("/api/artist-applications/{applicationId}", applicationId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.applicationId").value(applicationId))
                    .andDo(document("artist-applications-get",
                            pathParameters(
                                    parameterWithName("applicationId").description("신청 ID")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지"),
                                    fieldWithPath("data.applicationId").type(NUMBER).description("신청 ID"),
                                    fieldWithPath("data.requesterUserId").type(NUMBER).description("신청자 유저 ID"),
                                    fieldWithPath("data.requestedName").type(STRING).description("아티스트 이름"),
                                    fieldWithPath("data.artistType").type(STRING).description("아티스트 유형"),
                                    fieldWithPath("data.bio").type(STRING).description("아티스트 소개"),
                                    fieldWithPath("data.profileImageUrl").type(STRING).description("프로필 이미지 URL"),
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
        void getArtistApplication_fail_forbidden() throws Exception {
            Long applicationId = 10L;

            given(artistService.getArtistApplication(any(), eq(applicationId)))
                    .willThrow(new BusinessException(
                            ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_DETAIL_FORBIDDEN
                    ));

            mockMvc.perform(get("/api/artist-applications/{applicationId}", applicationId))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(
                            ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_DETAIL_FORBIDDEN.getMessage()
                    ))
                    .andDo(document("artist-applications-get-forbidden",
                            pathParameters(
                                    parameterWithName("applicationId").description("신청 ID")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("에러 메시지")
                            )
                    ));
        }

        @Test
        @DisplayName("존재하지 않는 신청 조회 시 404 반환")
        void getArtistApplication_fail_notFound() throws Exception {
            Long applicationId = 999L;

            given(artistService.getArtistApplication(any(), eq(applicationId)))
                    .willThrow(new BusinessException(
                            ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_NOT_FOUND
                    ));

            mockMvc.perform(get("/api/artist-applications/{applicationId}", applicationId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(
                            ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_NOT_FOUND.getMessage()
                    ))
                    .andDo(document("artist-applications-get-not-found",
                            pathParameters(
                                    parameterWithName("applicationId").description("신청 ID")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("에러 메시지")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("관리자 아티스트 신청 목록 조회 API")
    class GetArtistApplications {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("관리자 아티스트 신청 목록 조회 성공")
        void getArtistApplications_success() throws Exception {

            ArtistApplicationListResponse content = new ArtistApplicationListResponse(
                    1L,
                    1L,
                    "아이유",
                    ArtistType.SOLO.name(),
                    ApplicationStatus.PENDING.name(),
                    LocalDateTime.of(2026, 4, 18, 12, 0, 0)
            );

            PageResponse<ArtistApplicationListResponse> response =
                    PageResponse.from(
                            new PageImpl<>(List.of(content), PageRequest.of(0, 10), 1)
                    );

            given(artistService.getArtistApplications(eq(null), any(Pageable.class)))
                    .willReturn(response);

            mockMvc.perform(get("/api/admin/artist-applications"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].applicationId").value(1L))
                    .andDo(document("admin-artist-applications",
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지"),

                                    fieldWithPath("data.content").type(ARRAY).description("신청 목록"),
                                    fieldWithPath("data.content[].applicationId").type(NUMBER).description("신청 ID"),
                                    fieldWithPath("data.content[].requesterUserId").type(NUMBER).description("신청자 ID"),
                                    fieldWithPath("data.content[].requestedName").type(STRING).description("아티스트 이름"),
                                    fieldWithPath("data.content[].artistType").type(STRING).description("아티스트 유형"),
                                    fieldWithPath("data.content[].status").type(STRING).description("신청 상태"),
                                    fieldWithPath("data.content[].createdAt").type(STRING).description("생성 시각"),

                                    fieldWithPath("data.page").type(NUMBER).description("현재 페이지"),
                                    fieldWithPath("data.size").type(NUMBER).description("페이지 크기"),
                                    fieldWithPath("data.totalElements").type(NUMBER).description("전체 개수"),
                                    fieldWithPath("data.totalPages").type(NUMBER).description("전체 페이지 수")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("아티스트 신청 승인 API")
    class ApproveArtistApplication {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("아티스트 신청 승인 성공")
        void approveArtistApplication_success() throws Exception {
            Long applicationId = 10L;

            ArtistApplicationApproveResponse response = new ArtistApplicationApproveResponse(
                    applicationId,
                    100L,
                    ArtistType.SOLO.name(),
                    ApplicationStatus.APPROVED.name(),
                    1L,
                    LocalDateTime.of(2026, 4, 18, 12, 0, 0)
            );

            given(artistService.approveArtistApplication(any(), eq(applicationId)))
                    .willReturn(response);

            mockMvc.perform(post("/api/admin/artist-applications/{applicationId}/approve", applicationId)
                            .with(csrf().asHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.applicationId").value(applicationId))
                    .andDo(document("admin-artist-applications-approve",
                            pathParameters(
                                    parameterWithName("applicationId").description("신청 ID")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지"),
                                    fieldWithPath("data.applicationId").type(NUMBER).description("신청 ID"),
                                    fieldWithPath("data.artistId").type(NUMBER).description("생성된 아티스트 ID"),
                                    fieldWithPath("data.artistType").type(STRING).description("아티스트 유형"),
                                    fieldWithPath("data.status").type(STRING).description("신청 상태"),
                                    fieldWithPath("data.reviewedByAdminId").type(NUMBER).description("승인 관리자 ID"),
                                    fieldWithPath("data.reviewedAt").type(STRING).description("승인 시각")
                            )
                    ));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("이미 처리된 신청 승인 시 400 반환")
        void approveArtistApplication_fail_whenAlreadyProcessed() throws Exception {
            Long applicationId = 10L;

            given(artistService.approveArtistApplication(any(), eq(applicationId)))
                    .willThrow(new BusinessException(
                            ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_ALREADY_PROCESSED
                    ));

            mockMvc.perform(post("/api/admin/artist-applications/{applicationId}/approve", applicationId)
                            .with(csrf().asHeader()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_ALREADY_PROCESSED.getMessage()
                    ))
                    .andDo(document("admin-artist-applications-approve-already-processed",
                            pathParameters(
                                    parameterWithName("applicationId").description("신청 ID")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("에러 메시지")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("아티스트 신청 거절 API")
    class RejectArtistApplication {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("아티스트 신청 거절 성공")
        void rejectArtistApplication_success() throws Exception {
            Long applicationId = 10L;

            ArtistApplicationRejectRequest request =
                    new ArtistApplicationRejectRequest("정보 부족");

            ArtistApplicationRejectResponse response = new ArtistApplicationRejectResponse(
                    applicationId,
                    ArtistType.SOLO.name(),
                    ApplicationStatus.REJECTED.name(),
                    1L,
                    LocalDateTime.of(2026, 4, 18, 12, 0, 0),
                    "정보 부족"
            );

            given(artistService.rejectArtistApplication(any(), eq(applicationId), any(ArtistApplicationRejectRequest.class)))
                    .willReturn(response);

            mockMvc.perform(post("/api/admin/artist-applications/{applicationId}/reject", applicationId)
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.applicationId").value(applicationId))
                    .andDo(document("admin-artist-applications-reject",
                            pathParameters(
                                    parameterWithName("applicationId").description("신청 ID")
                            ),
                            requestFields(
                                    fieldWithPath("rejectionReason").type(STRING).description("거절 사유")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지"),
                                    fieldWithPath("data.applicationId").type(NUMBER).description("신청 ID"),
                                    fieldWithPath("data.artistType").type(STRING).description("아티스트 유형"),
                                    fieldWithPath("data.status").type(STRING).description("신청 상태"),
                                    fieldWithPath("data.reviewedByAdminId").type(NUMBER).description("거절 관리자 ID"),
                                    fieldWithPath("data.reviewedAt").type(STRING).description("거절 시각"),
                                    fieldWithPath("data.rejectionReason").type(STRING).description("거절 사유")
                            )
                    ));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("거절 사유 없이 요청하면 400 반환")
        void rejectArtistApplication_fail_withoutReason() throws Exception {
            ArtistApplicationRejectRequest request =
                    new ArtistApplicationRejectRequest("");

            mockMvc.perform(post("/api/admin/artist-applications/{applicationId}/reject", 10L)
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andDo(document("admin-artist-applications-reject-invalid",
                            requestFields(
                                    fieldWithPath("rejectionReason").type(STRING).description("거절 사유 (값 없음)")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("에러 메시지"),
                                    fieldWithPath("data").type(ARRAY).description("상세 에러 내역"),
                                    fieldWithPath("data[].field").type(STRING).description("에러가 발생한 필드명"),
                                    fieldWithPath("data[].message").type(STRING).description("에러 상세 사유")
                            )
                    ));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("이미 처리된 신청 거절 시 400 반환")
        void rejectArtistApplication_fail_whenAlreadyProcessed() throws Exception {
            Long applicationId = 10L;

            ArtistApplicationRejectRequest request =
                    new ArtistApplicationRejectRequest("정보 부족");

            given(artistService.rejectArtistApplication(any(), eq(applicationId), any(ArtistApplicationRejectRequest.class)))
                    .willThrow(new BusinessException(
                            ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_ALREADY_PROCESSED
                    ));

            mockMvc.perform(post("/api/admin/artist-applications/{applicationId}/reject", applicationId)
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_ALREADY_PROCESSED.getMessage()
                    ))
                    .andDo(document("admin-artist-applications-reject-already-processed",
                            pathParameters(
                                    parameterWithName("applicationId").description("신청 ID")
                            ),
                            requestFields(
                                    fieldWithPath("rejectionReason").type(STRING).description("거절 사유")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("에러 메시지")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("내 아티스트 목록 조회 API")
    class GetMyArtists {

        @Test
        @DisplayName("내 아티스트 목록 조회 성공")
        void getMyArtists_success() throws Exception {
            List<MyArtistResponse> response = List.of(
                    new MyArtistResponse(
                            2L,
                            "아이유",
                            ArtistType.SOLO.name(),
                            "가수",
                            "https://example.com/iu.jpg",
                            LocalDateTime.of(2026, 4, 15, 10, 0, 0),
                            LocalDateTime.of(2026, 4, 15, 11, 0, 0)
                    ),
                    new MyArtistResponse(
                            1L,
                            "아이유 밴드",
                            ArtistType.COLLABORATION.name(),
                            "프로젝트 아티스트",
                            "https://example.com/band.jpg",
                            LocalDateTime.of(2026, 4, 14, 10, 0, 0),
                            LocalDateTime.of(2026, 4, 14, 11, 0, 0)
                    )
            );

            given(artistService.getMyArtists(any()))
                    .willReturn(response);

            mockMvc.perform(get("/api/my/artists"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].artistId").value(2L))
                    .andDo(document("my-artists-get",
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지"),
                                    fieldWithPath("data").type(ARRAY).description("내 아티스트 목록"),
                                    fieldWithPath("data[].artistId").type(NUMBER).description("아티스트 ID"),
                                    fieldWithPath("data[].name").type(STRING).description("아티스트 이름"),
                                    fieldWithPath("data[].artistType").type(STRING).description("아티스트 유형"),
                                    fieldWithPath("data[].bio").type(STRING).description("아티스트 소개"),
                                    fieldWithPath("data[].profileImageUrl").type(STRING).description("프로필 이미지 URL"),
                                    fieldWithPath("data[].createdAt").type(STRING).description("생성 시각"),
                                    fieldWithPath("data[].updatedAt").type(STRING).description("수정 시각")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("아티스트 상세 조회 API")
    class GetArtist {

        @Test
        @DisplayName("아티스트 상세 조회 성공")
        void getArtist_success() throws Exception {
            Long artistId = 1L;

            ArtistDetailResponse response = new ArtistDetailResponse(
                    artistId,
                    "아이유",
                    ArtistType.SOLO.name(),
                    ArtistStatus.ACTIVE.name(),
                    "가수",
                    "https://example.com/iu.jpg",
                    LocalDateTime.of(2026, 4, 15, 10, 0, 0),
                    LocalDateTime.of(2026, 4, 15, 11, 0, 0)
            );

            given(artistService.getArtist(artistId)).willReturn(response);

            mockMvc.perform(get("/api/artists/{artistId}", artistId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.artistId").value(artistId))
                    .andDo(document("artists-get",
                            pathParameters(
                                    parameterWithName("artistId").description("아티스트 ID")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지"),
                                    fieldWithPath("data.artistId").type(NUMBER).description("아티스트 ID"),
                                    fieldWithPath("data.name").type(STRING).description("아티스트 이름"),
                                    fieldWithPath("data.artistType").type(STRING).description("아티스트 유형"),
                                    fieldWithPath("data.status").type(STRING).description("아티스트 상태"),
                                    fieldWithPath("data.bio").type(STRING).description("아티스트 소개"),
                                    fieldWithPath("data.profileImageUrl").type(STRING).description("프로필 이미지 URL"),
                                    fieldWithPath("data.createdAt").type(STRING).description("생성 시각"),
                                    fieldWithPath("data.updatedAt").type(STRING).description("수정 시각")
                            )
                    ));
        }

        @Test
        @DisplayName("존재하지 않는 아티스트 조회 시 404 반환")
        void getArtist_fail_notFound() throws Exception {
            Long artistId = 999L;

            given(artistService.getArtist(artistId))
                    .willThrow(new BusinessException(
                            ArtistErrorCode.ERR_ARTIST_NOT_FOUND
                    ));

            mockMvc.perform(get("/api/artists/{artistId}", artistId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(
                            ArtistErrorCode.ERR_ARTIST_NOT_FOUND.getMessage()
                    ))
                    .andDo(document("artists-get-not-found",
                            pathParameters(
                                    parameterWithName("artistId").description("아티스트 ID")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("에러 메시지")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("아티스트 프로필 수정 API")
    class UpdateArtist {

        @Test
        @DisplayName("아티스트 프로필 수정 성공")
        void updateArtist_success() throws Exception {
            Long artistId = 1L;

            ArtistProfileUpdateRequest request = new ArtistProfileUpdateRequest(
                    "아이유 리브랜딩",
                    "대한민국 솔로 가수",
                    "https://example.com/new-iu.jpg"
            );

            ArtistDetailResponse response = new ArtistDetailResponse(
                    artistId,
                    "아이유 리브랜딩",
                    ArtistType.SOLO.name(),
                    ArtistStatus.ACTIVE.name(),
                    "대한민국 솔로 가수",
                    "https://example.com/new-iu.jpg",
                    LocalDateTime.of(2026, 4, 15, 10, 0, 0),
                    LocalDateTime.of(2026, 4, 15, 11, 0, 0)
            );

            given(artistService.updateArtistProfile(any(), eq(artistId), any(ArtistProfileUpdateRequest.class)))
                    .willReturn(response);

            mockMvc.perform(patch("/api/artists/{artistId}", artistId)
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.artistId").value(artistId))
                    .andDo(document("artists-update",
                            pathParameters(
                                    parameterWithName("artistId").description("아티스트 ID")
                            ),
                            requestFields(
                                    fieldWithPath("name").type(STRING).description("아티스트 이름"),
                                    fieldWithPath("bio").type(STRING).description("아티스트 소개"),
                                    fieldWithPath("profileImageUrl").type(STRING).description("프로필 이미지 URL")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지"),
                                    fieldWithPath("data.artistId").type(NUMBER).description("아티스트 ID"),
                                    fieldWithPath("data.name").type(STRING).description("아티스트 이름"),
                                    fieldWithPath("data.artistType").type(STRING).description("아티스트 유형"),
                                    fieldWithPath("data.status").type(STRING).description("아티스트 상태"),
                                    fieldWithPath("data.bio").type(STRING).description("아티스트 소개"),
                                    fieldWithPath("data.profileImageUrl").type(STRING).description("프로필 이미지 URL"),
                                    fieldWithPath("data.createdAt").type(STRING).description("생성 시각"),
                                    fieldWithPath("data.updatedAt").type(STRING).description("수정 시각")
                            )
                    ));
        }

        @Test
        @DisplayName("다른 사용자의 아티스트 수정 시 403 반환")
        void updateArtist_fail_forbidden() throws Exception {
            Long artistId = 1L;

            ArtistProfileUpdateRequest request = new ArtistProfileUpdateRequest(
                    "아이유 리브랜딩",
                    "대한민국 솔로 가수",
                    "https://example.com/new-iu.jpg"
            );

            given(artistService.updateArtistProfile(any(), eq(artistId), any(ArtistProfileUpdateRequest.class)))
                    .willThrow(new BusinessException(
                            ArtistErrorCode.ERR_FORBIDDEN_ARTIST_ACCESS
                    ));

            mockMvc.perform(patch("/api/artists/{artistId}", artistId)
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(
                            ArtistErrorCode.ERR_FORBIDDEN_ARTIST_ACCESS.getMessage()
                    ))
                    .andDo(document("artists-update-forbidden",
                            pathParameters(
                                    parameterWithName("artistId").description("아티스트 ID")
                            ),
                            requestFields(
                                    fieldWithPath("name").type(STRING).description("아티스트 이름"),
                                    fieldWithPath("bio").type(STRING).description("아티스트 소개"),
                                    fieldWithPath("profileImageUrl").type(STRING).description("프로필 이미지 URL")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("에러 메시지")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("아티스트 삭제 API")
    class DeleteArtist {

        @Test
        @DisplayName("아티스트 삭제 성공")
        void deleteArtist_success() throws Exception {

            mockMvc.perform(delete("/api/artists/{artistId}", 1L)
                            .with(csrf().asHeader()))
                    .andExpect(status().isOk())
                    .andDo(document("artists-delete",
                            pathParameters(
                                    parameterWithName("artistId").description("아티스트 ID")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지")
                            )
                    ));
        }

        @Test
        @DisplayName("존재하지 않는 아티스트 삭제 시 404 반환")
        void deleteArtist_fail_notFound() throws Exception {
            Long artistId = 999L;

            willThrow(new BusinessException(
                    ArtistErrorCode.ERR_ARTIST_NOT_FOUND
            )).given(artistService).deleteArtist(any(), eq(artistId));

            mockMvc.perform(delete("/api/artists/{artistId}", artistId)
                            .with(csrf().asHeader()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(
                            ArtistErrorCode.ERR_ARTIST_NOT_FOUND.getMessage()
                    ))
                    .andDo(document("artists-delete-not-found",
                            pathParameters(
                                    parameterWithName("artistId").description("아티스트 ID")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("에러 메시지")
                            )
                    ));
        }

        @Test
        @DisplayName("다른 사용자의 아티스트 삭제 시 403 반환")
        void deleteArtist_fail_forbidden() throws Exception {
            Long artistId = 1L;

            willThrow(new BusinessException(
                    ArtistErrorCode.ERR_FORBIDDEN_ARTIST_ACCESS
            )).given(artistService).deleteArtist(any(), eq(artistId));

            mockMvc.perform(delete("/api/artists/{artistId}", artistId)
                            .with(csrf().asHeader()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(
                            ArtistErrorCode.ERR_FORBIDDEN_ARTIST_ACCESS.getMessage()
                    ))
                    .andDo(document("artists-delete-forbidden",
                            pathParameters(
                                    parameterWithName("artistId").description("아티스트 ID")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("에러 메시지")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("아티스트 활성화 API")
    class ActivateArtist {

        @Test
        @DisplayName("아티스트 활성화 성공")
        void activateArtist_success() throws Exception {
            Long artistId = 1L;

            ArtistDetailResponse response = new ArtistDetailResponse(
                    artistId,
                    "아이유",
                    ArtistType.SOLO.name(),
                    ArtistStatus.ACTIVE.name(),
                    "가수",
                    "https://example.com/iu.jpg",
                    LocalDateTime.of(2026, 4, 15, 10, 0, 0),
                    LocalDateTime.of(2026, 4, 15, 11, 0, 0)
            );

            given(artistService.activateArtist(any(), eq(artistId)))
                    .willReturn(response);

            mockMvc.perform(patch("/api/artists/{artistId}/activate", artistId)
                            .with(csrf().asHeader()))
                    .andExpect(status().isOk())
                    .andDo(document("artists-activate",
                            pathParameters(
                                    parameterWithName("artistId").description("아티스트 ID")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지"),
                                    fieldWithPath("data.artistId").type(NUMBER).description("아티스트 ID"),
                                    fieldWithPath("data.name").type(STRING).description("아티스트 이름"),
                                    fieldWithPath("data.artistType").type(STRING).description("아티스트 유형"),
                                    fieldWithPath("data.status").type(STRING).description("아티스트 상태"),
                                    fieldWithPath("data.bio").type(STRING).description("아티스트 소개"),
                                    fieldWithPath("data.profileImageUrl").type(STRING).description("프로필 이미지 URL"),
                                    fieldWithPath("data.createdAt").type(STRING).description("생성 시각"),
                                    fieldWithPath("data.updatedAt").type(STRING).description("수정 시각")
                            )
                    ));
        }

        @Test
        @DisplayName("존재하지 않는 아티스트 활성화 시 404 반환")
        void activateArtist_fail_notFound() throws Exception {
            Long artistId = 999L;

            given(artistService.activateArtist(any(), eq(artistId)))
                    .willThrow(new BusinessException(
                            ArtistErrorCode.ERR_ARTIST_NOT_FOUND
                    ));

            mockMvc.perform(patch("/api/artists/{artistId}/activate", artistId)
                            .with(csrf().asHeader()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(
                            ArtistErrorCode.ERR_ARTIST_NOT_FOUND.getMessage()
                    ))
                    .andDo(document("artists-activate-not-found",
                            pathParameters(
                                    parameterWithName("artistId").description("아티스트 ID")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("에러 메시지")
                            )
                    ));
        }

        @Test
        @DisplayName("이미 활성화된 아티스트 활성화 시 400 반환")
        void activateArtist_fail_alreadyActivated() throws Exception {
            Long artistId = 1L;

            given(artistService.activateArtist(any(), eq(artistId)))
                    .willThrow(new BusinessException(
                            ArtistErrorCode.ERR_ARTIST_ALREADY_ACTIVATED
                    ));

            mockMvc.perform(patch("/api/artists/{artistId}/activate", artistId)
                            .with(csrf().asHeader()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            ArtistErrorCode.ERR_ARTIST_ALREADY_ACTIVATED.getMessage()
                    ))
                    .andDo(document("artists-activate-already-activated",
                            pathParameters(
                                    parameterWithName("artistId").description("아티스트 ID")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("에러 메시지")
                            )
                    ));
        }

        @Test
        @DisplayName("다른 사용자의 아티스트 활성화 시 403 반환")
        void activateArtist_fail_forbidden() throws Exception {
            Long artistId = 1L;

            given(artistService.activateArtist(any(), eq(artistId)))
                    .willThrow(new BusinessException(
                            ArtistErrorCode.ERR_FORBIDDEN_ARTIST_ACCESS
                    ));

            mockMvc.perform(patch("/api/artists/{artistId}/activate", artistId)
                            .with(csrf().asHeader()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(
                            ArtistErrorCode.ERR_FORBIDDEN_ARTIST_ACCESS.getMessage()
                    ))
                    .andDo(document("artists-activate-forbidden",
                            pathParameters(
                                    parameterWithName("artistId").description("아티스트 ID")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("에러 메시지")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("아티스트 비활성화 API")
    class DeactivateArtist {

        @Test
        @DisplayName("아티스트 비활성화 성공")
        void deactivateArtist_success() throws Exception {
            Long artistId = 1L;

            ArtistDetailResponse response = new ArtistDetailResponse(
                    artistId,
                    "아이유",
                    ArtistType.SOLO.name(),
                    ArtistStatus.INACTIVE.name(),
                    "가수",
                    "https://example.com/iu.jpg",
                    LocalDateTime.of(2026, 4, 15, 10, 0, 0),
                    LocalDateTime.of(2026, 4, 15, 11, 0, 0)
            );

            given(artistService.deactivateArtist(any(), eq(artistId)))
                    .willReturn(response);

            mockMvc.perform(patch("/api/artists/{artistId}/deactivate", artistId)
                            .with(csrf().asHeader()))
                    .andExpect(status().isOk())
                    .andDo(document("artists-deactivate",
                            pathParameters(
                                    parameterWithName("artistId").description("아티스트 ID")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지"),
                                    fieldWithPath("data.artistId").type(NUMBER).description("아티스트 ID"),
                                    fieldWithPath("data.name").type(STRING).description("아티스트 이름"),
                                    fieldWithPath("data.artistType").type(STRING).description("아티스트 유형"),
                                    fieldWithPath("data.status").type(STRING).description("아티스트 상태"),
                                    fieldWithPath("data.bio").type(STRING).description("아티스트 소개"),
                                    fieldWithPath("data.profileImageUrl").type(STRING).description("프로필 이미지 URL"),
                                    fieldWithPath("data.createdAt").type(STRING).description("생성 시각"),
                                    fieldWithPath("data.updatedAt").type(STRING).description("수정 시각")
                            )
                    ));
        }

        @Test
        @DisplayName("존재하지 않는 아티스트 비활성화 시 404 반환")
        void deactivateArtist_fail_notFound() throws Exception {
            Long artistId = 999L;

            given(artistService.deactivateArtist(any(), eq(artistId)))
                    .willThrow(new BusinessException(
                            ArtistErrorCode.ERR_ARTIST_NOT_FOUND
                    ));

            mockMvc.perform(patch("/api/artists/{artistId}/deactivate", artistId)
                            .with(csrf().asHeader()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(
                            ArtistErrorCode.ERR_ARTIST_NOT_FOUND.getMessage()
                    ))
                    .andDo(document("artists-deactivate-not-found",
                            pathParameters(
                                    parameterWithName("artistId").description("아티스트 ID")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("에러 메시지")
                            )
                    ));
        }

        @Test
        @DisplayName("이미 비활성화된 아티스트 비활성화 시 400 반환")
        void deactivateArtist_fail_alreadyInactive() throws Exception {
            Long artistId = 1L;

            given(artistService.deactivateArtist(any(), eq(artistId)))
                    .willThrow(new BusinessException(
                            ArtistErrorCode.ERR_ARTIST_ALREADY_INACTIVE
                    ));

            mockMvc.perform(patch("/api/artists/{artistId}/deactivate", artistId)
                            .with(csrf().asHeader()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            ArtistErrorCode.ERR_ARTIST_ALREADY_INACTIVE.getMessage()
                    ))
                    .andDo(document("artists-deactivate-already-inactive",
                            pathParameters(
                                    parameterWithName("artistId").description("아티스트 ID")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("에러 메시지")
                            )
                    ));
        }

        @Test
        @DisplayName("다른 사용자의 아티스트 비활성화 시 403 반환")
        void deactivateArtist_fail_forbidden() throws Exception {
            Long artistId = 1L;

            given(artistService.deactivateArtist(any(), eq(artistId)))
                    .willThrow(new BusinessException(
                            ArtistErrorCode.ERR_FORBIDDEN_ARTIST_ACCESS
                    ));

            mockMvc.perform(patch("/api/artists/{artistId}/deactivate", artistId)
                            .with(csrf().asHeader()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(
                            ArtistErrorCode.ERR_FORBIDDEN_ARTIST_ACCESS.getMessage()
                    ))
                    .andDo(document("artists-deactivate-forbidden",
                            pathParameters(
                                    parameterWithName("artistId").description("아티스트 ID")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("에러 메시지")
                            )
                    ));
        }
    }
}