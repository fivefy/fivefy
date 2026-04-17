package com.fivefy.domain.playlist.controller;

import com.fivefy.common.config.security.JwtUtil;
import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.playlist.contoller.PlaylistController;
import com.fivefy.domain.playlist.dto.request.PlaylistCreateRequest;
import com.fivefy.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.fivefy.domain.playlist.dto.response.PlaylistDeleteResponse;
import com.fivefy.domain.playlist.dto.response.PlaylistResponse;
import com.fivefy.domain.playlist.enums.PlaylistErrorCode;
import com.fivefy.domain.playlist.service.PlaylistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlaylistController.class)
@AutoConfigureMockMvc(addFilters = false)
class PlaylistControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private PlaylistService playlistService;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private StringRedisTemplate stringRedisTemplate;

    @Nested
    @DisplayName("플레이리스트 생성")
    class CreatePlaylist {

        @Test
        @DisplayName("플레이리스트 생성 성공 시 201 반환")
        void createPlaylistSuccess() throws Exception {
            // given
            PlaylistCreateRequest request = new PlaylistCreateRequest("운동할 때 듣는 노래", "신나는 음악 모음");
            PlaylistResponse response = new PlaylistResponse(
                    1L,
                    100L,
                    "운동할 때 듣는 노래",
                    "신나는 음악 모음",
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    null
            );

            given(playlistService.createPlaylist(any(), any(PlaylistCreateRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/playlists")
                            .param("userId", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("플레이리스트 생성 성공"))
                    .andExpect(jsonPath("$.data.id").value(1L))
                    .andExpect(jsonPath("$.data.title").value("운동할 때 듣는 노래"))
                    .andExpect(jsonPath("$.data.description").value("신나는 음악 모음"));
        }

        @Test
        @DisplayName("제목 없이 생성 요청 시 400 반환")
        void createPlaylistWithoutTitle() throws Exception {
            // given
            PlaylistCreateRequest request = new PlaylistCreateRequest("", "설명");

            // when & then
            mockMvc.perform(post("/api/playlists")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("제목이 100자를 초과하면 400 반환")
        void createPlaylistWithTooLongTitle() throws Exception {
            // given
            String longTitle = "a".repeat(101);
            PlaylistCreateRequest request = new PlaylistCreateRequest(longTitle, "설명");

            // when & then
            mockMvc.perform(post("/api/playlists")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("중복 제목으로 생성 시 409 반환")
        void createPlaylistWithDuplicateName() throws Exception {
            // given
            PlaylistCreateRequest request = new PlaylistCreateRequest("중복 제목", "설명");

            given(playlistService.createPlaylist(any(), any(PlaylistCreateRequest.class)))
                    .willThrow(new BusinessException(PlaylistErrorCode.DUPLICATE_PLAYLIST_NAME));

            // when & then
            mockMvc.perform(post("/api/playlists")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(PlaylistErrorCode.DUPLICATE_PLAYLIST_NAME.getMessage()));
        }
    }

    @Nested
    @DisplayName("플레이리스트 목록 조회")
    class GetPlaylists {

        @Test
        @DisplayName("플레이리스트 목록 조회 성공 시 200 반환")
        void getPlaylistsSuccess() throws Exception {
            // given
            PlaylistResponse playlist1 = new PlaylistResponse(
                    1L, 100L, "플리1", "설명1",
                    LocalDateTime.now(), LocalDateTime.now(), null
            );
            PlaylistResponse playlist2 = new PlaylistResponse(
                    2L, 101L, "플리2", "설명2",
                    LocalDateTime.now(), LocalDateTime.now(), null
            );

            PageResponse<PlaylistResponse> response = new PageResponse<>(
                    List.of(playlist1, playlist2),
                    0,
                    20,
                    2L,
                    1
            );

            given(playlistService.getPlaylists(any(Pageable.class))).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/playlists")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("플레이리스트 목록 조회 성공"))
                    .andExpect(jsonPath("$.data.content.length()").value(2))
                    .andExpect(jsonPath("$.data.content[0].title").value("플리1"))
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(20))
                    .andExpect(jsonPath("$.data.totalElements").value(2))
                    .andExpect(jsonPath("$.data.totalPages").value(1));
        }
    }

    @Nested
    @DisplayName("플레이리스트 단건 조회")
    class GetPlaylist {

        @Test
        @DisplayName("플레이리스트 조회 성공 시 200 반환")
        void getPlaylistSuccess() throws Exception {
            // given
            PlaylistResponse response = new PlaylistResponse(
                    1L,
                    100L,
                    "내 플레이리스트",
                    "설명",
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    null
            );

            given(playlistService.getPlaylist(1L)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/playlists/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("플레이리스트 조회 성공"))
                    .andExpect(jsonPath("$.data.id").value(1L))
                    .andExpect(jsonPath("$.data.title").value("내 플레이리스트"));
        }

        @Test
        @DisplayName("존재하지 않는 플레이리스트 조회 시 404 반환")
        void getPlaylistNotFound() throws Exception {
            // given
            given(playlistService.getPlaylist(1L))
                    .willThrow(new BusinessException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/playlists/1"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(PlaylistErrorCode.PLAYLIST_NOT_FOUND.getMessage()));
        }
    }

    @Nested
    @DisplayName("플레이리스트 수정")
    class UpdatePlaylist {

        @Test
        @DisplayName("플레이리스트 수정 성공 시 200 반환")
        void updatePlaylistSuccess() throws Exception {
            // given
            PlaylistUpdateRequest request = new PlaylistUpdateRequest("수정된 제목", "수정된 설명");
            PlaylistResponse response = new PlaylistResponse(
                    1L,
                    100L,
                    "수정된 제목",
                    "수정된 설명",
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    null
            );

            given(playlistService.updatePlaylist(any(), eq(1L), any(PlaylistUpdateRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(patch("/api/playlists/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("플레이리스트 수정 성공"))
                    .andExpect(jsonPath("$.data.title").value("수정된 제목"))
                    .andExpect(jsonPath("$.data.description").value("수정된 설명"));
        }

        @Test
        @DisplayName("제목 없이 수정 요청 시 400 반환")
        void updatePlaylistWithoutTitle() throws Exception {
            // given
            PlaylistUpdateRequest request = new PlaylistUpdateRequest("", "설명");

            // when & then
            mockMvc.perform(patch("/api/playlists/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("본인 소유가 아닌 플레이리스트 수정 시 403 반환")
        void updatePlaylistForbidden() throws Exception {
            // given
            PlaylistUpdateRequest request = new PlaylistUpdateRequest("수정 제목", "설명");

            given(playlistService.updatePlaylist(any(), eq(1L), any(PlaylistUpdateRequest.class)))
                    .willThrow(new BusinessException(PlaylistErrorCode.PLAYLIST_UPDATE_FORBIDDEN));

            // when & then
            mockMvc.perform(patch("/api/playlists/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(PlaylistErrorCode.PLAYLIST_UPDATE_FORBIDDEN.getMessage()));
        }

        @Test
        @DisplayName("존재하지 않는 플레이리스트 수정 시 404 반환")
        void updatePlaylistNotFound() throws Exception {
            // given
            PlaylistUpdateRequest request = new PlaylistUpdateRequest("수정 제목", "설명");

            given(playlistService.updatePlaylist(any(), eq(1L), any(PlaylistUpdateRequest.class)))
                    .willThrow(new BusinessException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

            // when & then
            mockMvc.perform(patch("/api/playlists/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(PlaylistErrorCode.PLAYLIST_NOT_FOUND.getMessage()));
        }

        @Test
        @DisplayName("중복 제목으로 수정 시 409 반환")
        void updatePlaylistDuplicateName() throws Exception {
            // given
            PlaylistUpdateRequest request = new PlaylistUpdateRequest("중복 제목", "설명");

            given(playlistService.updatePlaylist(any(), eq(1L), any(PlaylistUpdateRequest.class)))
                    .willThrow(new BusinessException(PlaylistErrorCode.DUPLICATE_PLAYLIST_NAME));

            // when & then
            mockMvc.perform(patch("/api/playlists/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(PlaylistErrorCode.DUPLICATE_PLAYLIST_NAME.getMessage()));
        }
    }

    @Nested
    @DisplayName("플레이리스트 삭제")
    class DeletePlaylist {

        @Test
        @DisplayName("플레이리스트 삭제 성공 시 200 반환")
        void deletePlaylistSuccess() throws Exception {
            // given
            PlaylistDeleteResponse response = new PlaylistDeleteResponse(
                    1L,
                    LocalDateTime.now()
            );

            given(playlistService.deletePlaylist(any(), eq(1L))).willReturn(response);

            // when & then
            mockMvc.perform(delete("/api/playlists/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("플레이리스트 삭제 성공"))
                    .andExpect(jsonPath("$.data.id").value(1L));
        }

        @Test
        @DisplayName("본인 소유가 아닌 플레이리스트 삭제 시 403 반환")
        void deletePlaylistForbidden() throws Exception {
            // given
            given(playlistService.deletePlaylist(any(), eq(1L)))
                    .willThrow(new BusinessException(PlaylistErrorCode.PLAYLIST_DELETE_FORBIDDEN));

            // when & then
            mockMvc.perform(delete("/api/playlists/1"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(PlaylistErrorCode.PLAYLIST_DELETE_FORBIDDEN.getMessage()));
        }

        @Test
        @DisplayName("존재하지 않는 플레이리스트 삭제 시 404 반환")
        void deletePlaylistNotFound() throws Exception {
            // given
            given(playlistService.deletePlaylist(any(), eq(1L)))
                    .willThrow(new BusinessException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

            // when & then
            mockMvc.perform(delete("/api/playlists/1"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(PlaylistErrorCode.PLAYLIST_NOT_FOUND.getMessage()));
        }
    }
}
