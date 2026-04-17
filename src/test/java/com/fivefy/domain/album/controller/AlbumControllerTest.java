package com.fivefy.domain.album.controller;

import com.fivefy.common.config.security.JwtUtil;
import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.album.dto.request.AlbumApplicationCreateRequest;
import com.fivefy.domain.album.dto.request.AlbumApplicationRejectRequest;
import com.fivefy.domain.album.dto.response.AlbumApplicationApproveResponse;
import com.fivefy.domain.album.dto.response.AlbumApplicationDetailResponse;
import com.fivefy.domain.album.dto.response.AlbumApplicationListResponse;
import com.fivefy.domain.album.dto.response.AlbumApplicationRejectResponse;
import com.fivefy.domain.album.dto.response.AlbumApplicationResponse;
import com.fivefy.domain.album.dto.response.AlbumDetailResponse;
import com.fivefy.domain.album.dto.response.ArtistAlbumListResponse;
import com.fivefy.domain.album.service.AlbumService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

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
 * AlbumController 웹 계층 테스트
 */
@WebMvcTest(AlbumController.class)
@AutoConfigureMockMvc(addFilters = false)
class AlbumControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AlbumService albumService;

    @MockitoBean
    private JwtUtil jwtUtil;

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
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("앨범 등록 신청 성공"))
                    .andExpect(jsonPath("$.data.applicationId").value(1L))
                    .andExpect(jsonPath("$.data.artistId").value(10L))
                    .andExpect(jsonPath("$.data.title").value("Love poem"))
                    .andExpect(jsonPath("$.data.status").value("PENDING"));
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
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
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
                    .andExpect(jsonPath("$.data[1].applicationId").value(1L));
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
                    .andExpect(jsonPath("$.data.title").value("Palette"));
        }
    }

    @Nested
    @DisplayName("관리자 앨범 등록 신청 목록 조회 API")
    class GetAlbumApplications {

        @Test
        @DisplayName("관리자 앨범 등록 신청 목록 조회 성공 시 200 반환")
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
                    .andExpect(jsonPath("$.data.content[0].title").value("첫 번째 신청"));
        }
    }

    @Nested
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
            mockMvc.perform(post("/api/admin/album-applications/{applicationId}/approve", applicationId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("앨범 등록 신청 승인 성공"))
                    .andExpect(jsonPath("$.data.applicationId").value(applicationId))
                    .andExpect(jsonPath("$.data.albumId").value(1000L))
                    .andExpect(jsonPath("$.data.status").value("APPROVED"));
        }
    }

    @Nested
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
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("앨범 등록 신청 거절 성공"))
                    .andExpect(jsonPath("$.data.applicationId").value(applicationId))
                    .andExpect(jsonPath("$.data.status").value("REJECTED"))
                    .andExpect(jsonPath("$.data.rejectionReason").value("앨범 정보가 부족합니다"));
        }

        @Test
        @DisplayName("거절 사유 없이 요청 시 400 반환")
        void rejectAlbumApplication_fail_withoutReason() throws Exception {
            // given
            AlbumApplicationRejectRequest request =
                    new AlbumApplicationRejectRequest("");

            // when & then
            mockMvc.perform(post("/api/admin/album-applications/10/reject")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
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
                    .andExpect(jsonPath("$.data.title").value("Palette"));
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
                    .andExpect(jsonPath("$.data[1].albumId").value(101L));
        }
    }
}