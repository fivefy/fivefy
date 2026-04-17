package com.fivefy.domain.album.controller;

import com.fivefy.common.config.security.JwtUtil;
import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.common.filter.LastActiveAtFilter;
import com.fivefy.domain.album.dto.request.AlbumApplicationCreateRequest;
import com.fivefy.domain.album.dto.request.AlbumApplicationRejectRequest;
import com.fivefy.domain.album.dto.response.*;
import com.fivefy.domain.album.service.AlbumService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import com.fivefy.common.docs.RestDocsSupport;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AlbumController 웹 계층 테스트 및 REST Docs 문서화
 */
@WithMockUser
@WebMvcTest(AlbumController.class)
class AlbumControllerTest extends RestDocsSupport {

    @MockitoBean
    private AlbumService albumService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private LastActiveAtFilter lastActiveAtFilter;

    @Nested
    @DisplayName("앨범 등록 신청 생성 API")
    class CreateAlbumApplication {

        @Test
        @DisplayName("앨범 등록 신청 생성 성공 시 201 반환")
        void createAlbumApplication_success() throws Exception {
            // given
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

            // when & then
            mockMvc.perform(post("/api/album-applications")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("앨범 등록 신청 성공"))
                    .andExpect(jsonPath("$.data.applicationId").value(1L))
                    .andExpect(jsonPath("$.data.artistId").value(10L))
                    .andExpect(jsonPath("$.data.title").value("Love poem"))
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andDo(document("album-applications-create",
                            requestFields(
                                    fieldWithPath("artistId").type(NUMBER).description("아티스트 ID"),
                                    fieldWithPath("title").type(STRING).description("앨범 제목"),
                                    fieldWithPath("description").type(STRING).description("앨범 설명"),
                                    fieldWithPath("coverImageUrl").type(STRING).description("커버 이미지 URL"),
                                    fieldWithPath("publishDelayDays").type(NUMBER).description("공개 예약 일수 (0~7)")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지"),
                                    fieldWithPath("data.applicationId").type(NUMBER).description("신청 ID"),
                                    fieldWithPath("data.artistId").type(NUMBER).description("아티스트 ID"),
                                    fieldWithPath("data.title").type(STRING).description("앨범 제목"),
                                    fieldWithPath("data.status").type(STRING).description("신청 상태"),
                                    fieldWithPath("data.createdAt").type(STRING).description("신청 생성 시각")
                            )
                    ));
        }

        @Test
        @DisplayName("제목 없이 생성 요청 시 400 반환")
        void createAlbumApplication_fail_withoutTitle() throws Exception {
            // given
            AlbumApplicationCreateRequest request = new AlbumApplicationCreateRequest(
                    10L,
                    "",
                    "앨범 설명",
                    "https://example.com/cover.jpg",
                    3
            );

            // when & then
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
                                    fieldWithPath("publishDelayDays").type(NUMBER).description("공개 예약 일수")
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
    }

    @Nested
    @DisplayName("내 앨범 등록 신청 목록 조회 API")
    class GetMyAlbumApplications {

        @Test
        @DisplayName("내 앨범 등록 신청 목록 조회 성공 시 200 반환")
        void getMyAlbumApplications_success() throws Exception {
            // given
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

            // when & then
            mockMvc.perform(get("/api/album-applications/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("내 앨범 등록 신청 목록 조회 성공"))
                    .andExpect(jsonPath("$.data[0].applicationId").value(2L))
                    .andExpect(jsonPath("$.data[0].title").value("두 번째 신청"))
                    .andExpect(jsonPath("$.data[1].applicationId").value(1L))
                    .andExpect(jsonPath("$.data[1].applicationId").value(1L))
                    .andDo(document("album-applications-me-get",
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지"),
                                    fieldWithPath("data").type(ARRAY).description("내 앨범 신청 목록"),
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
    @DisplayName("앨범 등록 신청 상세 조회 API")
    class GetAlbumApplication {

        @Test
        @DisplayName("앨범 등록 신청 상세 조회 성공 시 200 반환")
        void getAlbumApplication_success() throws Exception {
            // given
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

            // when & then
            mockMvc.perform(get("/api/album-applications/{applicationId}", applicationId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("앨범 등록 신청 상세 조회 성공"))
                    .andExpect(jsonPath("$.data.applicationId").value(applicationId))
                    .andExpect(jsonPath("$.data.title").value("Palette"))
                    .andExpect(jsonPath("$.data.title").value("Palette"))
                    .andDo(document("album-applications-get",
                            pathParameters(
                                    parameterWithName("applicationId").description("신청 ID")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지"),
                                    fieldWithPath("data.applicationId").type(NUMBER).description("신청 ID"),
                                    fieldWithPath("data.requesterUserId").type(NUMBER).description("신청자 유저 ID"),
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
                            )
                    ));
        }
    }

    @Nested
    @WithMockUser(roles = "ADMIN")
    @DisplayName("앨범 등록 신청 목록 조회 API")
    class GetAlbumApplications {

        @Test
        @DisplayName("앨범 등록 신청 목록 조회 성공 시 200 반환")
        void getAlbumApplications_success() throws Exception {
            // given
            AlbumApplicationListResponse content = new AlbumApplicationListResponse(
                    1L,
                    1L,
                    10L,
                    "첫 번째 신청",
                    ApplicationStatus.PENDING,
                    LocalDateTime.of(2026, 4, 14, 15, 0, 0)
            );

            PageResponse<AlbumApplicationListResponse> response = PageResponse.from(
                    new PageImpl<>(List.of(content))
            );

            given(albumService.getAlbumApplications(eq(null), any(Pageable.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/admin/album-applications"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("앨범 등록 신청 목록 조회 성공"))
                    .andExpect(jsonPath("$.data.content[0].applicationId").value(1L))
                    .andExpect(jsonPath("$.data.content[0].title").value("첫 번째 신청"))
                    .andExpect(jsonPath("$.data.content[0].title").value("첫 번째 신청"))
                    .andDo(document("admin-album-applications-get",
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지"),
                                    fieldWithPath("data.content").type(ARRAY).description("신청 목록"),
                                    fieldWithPath("data.content[].applicationId").type(NUMBER).description("신청 ID"),
                                    fieldWithPath("data.content[].requesterUserId").type(NUMBER).description("신청자 유저 ID"),
                                    fieldWithPath("data.content[].artistId").type(NUMBER).description("아티스트 ID"),
                                    fieldWithPath("data.content[].title").type(STRING).description("앨범 제목"),
                                    fieldWithPath("data.content[].status").type(STRING).description("신청 상태"),
                                    fieldWithPath("data.content[].createdAt").type(STRING).description("신청 생성 시각"),
                                    fieldWithPath("data.page").type(NUMBER).description("현재 페이지 번호"),
                                    fieldWithPath("data.size").type(NUMBER).description("페이지 크기"),
                                    fieldWithPath("data.totalElements").type(NUMBER).description("전체 데이터 수"),
                                    fieldWithPath("data.totalPages").type(NUMBER).description("전체 페이지 수")
                            )
                    ));
        }
    }

    @Nested
    @WithMockUser(roles = "ADMIN")
    @DisplayName("앨범 등록 신청 승인 API")
    class ApproveAlbumApplication {

        @Test
        @DisplayName("앨범 등록 신청 승인 성공 시 200 반환")
        void approveAlbumApplication_success() throws Exception {
            // given
            Long applicationId = 10L;

            AlbumApplicationApproveResponse response = new AlbumApplicationApproveResponse(
                    applicationId,
                    1000L,
                    "APPROVED",
                    1L,
                    LocalDateTime.of(2026, 4, 17, 12, 0, 0)
            );

            given(albumService.approveAlbumApplication(any(), eq(applicationId)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/admin/album-applications/{applicationId}/approve", applicationId)
                    .with(csrf().asHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("앨범 등록 신청 승인 성공"))
                    .andExpect(jsonPath("$.data.applicationId").value(applicationId))
                    .andExpect(jsonPath("$.data.albumId").value(1000L))
                    .andExpect(jsonPath("$.data.status").value("APPROVED"))
                    .andExpect(jsonPath("$.data.status").value("APPROVED"))
                    .andDo(document("admin-album-applications-approve",
                            pathParameters(
                                    parameterWithName("applicationId").description("신청 ID")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지"),
                                    fieldWithPath("data.applicationId").type(NUMBER).description("신청 ID"),
                                    fieldWithPath("data.albumId").type(NUMBER).description("생성된 앨범 ID"),
                                    fieldWithPath("data.status").type(STRING).description("신청 상태"),
                                    fieldWithPath("data.reviewedByAdminId").type(NUMBER).description("승인 관리자 ID"),
                                    fieldWithPath("data.reviewedAt").type(STRING).description("승인 시각")
                            )
                    ));
        }
    }

    @Nested
    @WithMockUser(roles = "ADMIN")
    @DisplayName("앨범 등록 신청 거절 API")
    class RejectAlbumApplication {

        @Test
        @DisplayName("앨범 등록 신청 거절 성공 시 200 반환")
        void rejectAlbumApplication_success() throws Exception {
            // given
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

            // when & then
            mockMvc.perform(post("/api/admin/album-applications/{applicationId}/reject", applicationId)
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("앨범 등록 신청 거절 성공"))
                    .andExpect(jsonPath("$.data.applicationId").value(applicationId))
                    .andExpect(jsonPath("$.data.status").value("REJECTED"))
                    .andExpect(jsonPath("$.data.rejectionReason").value("앨범 정보가 부족합니다"))
                    .andExpect(jsonPath("$.data.rejectionReason").value("앨범 정보가 부족합니다"))
                    .andDo(document("admin-album-applications-reject",
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
            // given
            AlbumApplicationRejectRequest request =
                    new AlbumApplicationRejectRequest("");

            // when & then
            mockMvc.perform(post("/api/admin/album-applications/10/reject")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(status().isBadRequest())
                    .andDo(document("admin-album-applications-reject-invalid",
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
    }

    @Nested
    @DisplayName("앨범 상세 조회 API")
    class GetAlbum {

        @Test
        @DisplayName("앨범 상세 조회 성공 시 200 반환")
        void getAlbum_success() throws Exception {
            // given
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
                    LocalDateTime.of(2026, 5, 1, 18, 0, 0)
            );

            given(albumService.getAlbum(albumId)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/albums/{albumId}", albumId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("앨범 상세 조회 성공"))
                    .andExpect(jsonPath("$.data.albumId").value(albumId))
                    .andExpect(jsonPath("$.data.artistName").value("아이유"))
                    .andExpect(jsonPath("$.data.title").value("Palette"))
                    .andExpect(jsonPath("$.data.title").value("Palette"))
                    .andDo(document("albums-get",
                            pathParameters(
                                    parameterWithName("albumId").description("앨범 ID")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지"),
                                    fieldWithPath("data.albumId").type(NUMBER).description("앨범 ID"),
                                    fieldWithPath("data.artistId").type(NUMBER).description("아티스트 ID"),
                                    fieldWithPath("data.artistName").type(STRING).description("아티스트 이름"),
                                    fieldWithPath("data.title").type(STRING).description("앨범 제목"),
                                    fieldWithPath("data.description").type(STRING).description("앨범 설명"),
                                    fieldWithPath("data.coverImageUrl").type(STRING).description("커버 이미지 URL"),
                                    fieldWithPath("data.trackCount").type(NUMBER).description("트랙 수"),
                                    fieldWithPath("data.totalDurationSec").type(NUMBER).description("총 재생 시간(초)"),
                                    fieldWithPath("data.publishedAt").type(STRING).description("게시 시각")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("아티스트별 앨범 목록 조회 API")
    class GetArtistAlbums {

        @Test
        @DisplayName("아티스트별 앨범 목록 조회 성공 시 200 반환")
        void getArtistAlbums_success() throws Exception {
            // given
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

            // when & then
            mockMvc.perform(get("/api/artists/{artistId}/albums", artistId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("아티스트별 앨범 목록 조회 성공"))
                    .andExpect(jsonPath("$.data[0].albumId").value(100L))
                    .andExpect(jsonPath("$.data[0].title").value("Palette"))
                    .andExpect(jsonPath("$.data[1].albumId").value(101L))
                    .andExpect(jsonPath("$.data[1].albumId").value(101L))
                    .andDo(document("artists-albums-get",
                            pathParameters(
                                    parameterWithName("artistId").description("아티스트 ID")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지"),
                                    fieldWithPath("data").type(ARRAY).description("앨범 목록"),
                                    fieldWithPath("data[].albumId").type(NUMBER).description("앨범 ID"),
                                    fieldWithPath("data[].title").type(STRING).description("앨범 제목"),
                                    fieldWithPath("data[].coverImageUrl").type(STRING).description("커버 이미지 URL"),
                                    fieldWithPath("data[].trackCount").type(NUMBER).description("트랙 수")
                            )
                    ));
        }
    }
}