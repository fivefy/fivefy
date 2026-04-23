package com.fivefy.domain.album.controller;

import com.fivefy.common.config.security.*;
import com.fivefy.common.filter.LastActiveAtFilter;
import com.fivefy.domain.album.dto.response.AlbumDetailResponse;
import com.fivefy.domain.album.dto.response.AlbumTrackResponse;
import com.fivefy.domain.album.dto.response.ArtistAlbumListResponse;
import com.fivefy.domain.album.service.AlbumService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlbumController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, JwtFilter.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class, LastActiveAtFilter.class})
class AlbumSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlbumService albumService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @Nested
    @DisplayName("공개 API 접근")
    class PublicEndpoints {

        @Test
        @DisplayName("앨범 상세 조회는 익명 접근 허용")
        void getAlbum_allowAnonymous() throws Exception {
            given(albumService.getAlbum(1L)).willReturn(
                    new AlbumDetailResponse(
                            1L,
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
                    )
            );

            mockMvc.perform(get("/api/albums/{albumId}", 1L))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("아티스트별 앨범 목록 조회는 익명 접근 허용")
        void getArtistAlbums_allowAnonymous() throws Exception {
            given(albumService.getArtistAlbums(10L)).willReturn(
                    List.of(
                            new ArtistAlbumListResponse(
                                    100L,
                                    "Palette",
                                    "https://example.com/album1.jpg",
                                    10L
                            )
                    )
            );

            mockMvc.perform(get("/api/artists/{artistId}/albums", 10L))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("관리자 API 접근")
    class AdminEndpoints {

        @Test
        @DisplayName("앨범 등록 신청 승인 API는 익명 접근 차단")
        void approveAlbumApplication_denyAnonymous() throws Exception {
            mockMvc.perform(post("/api/admin/album-applications/{applicationId}/approve", 10L))
                    .andExpect(status().isUnauthorized());
        }
    }
}