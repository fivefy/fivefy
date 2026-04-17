package com.fivefy.domain.playlisttrack.controller;

import com.fivefy.common.config.security.JwtUtil;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.playlist.enums.PlaylistErrorCode;
import com.fivefy.domain.playlisttrack.dto.request.PlaylistTrackCreateRequest;
import com.fivefy.domain.playlisttrack.dto.request.PlaylistTrackOrderUpdateRequest;
import com.fivefy.domain.playlisttrack.dto.response.PlaylistTrackResponse;
import com.fivefy.domain.playlisttrack.service.PlaylistTrackService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlaylistTrackController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PlaylistTrackControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private PlaylistTrackService playlistTrackService;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private StringRedisTemplate stringRedisTemplate;

    @Nested
    @DisplayName("플레이리스트 트랙 추가")
    class AddTrack {

        @Test
        @DisplayName("플레이리스트 트랙 추가 성공 시 201 반환")
        void addTrackSuccess() throws Exception {
            // given
            PlaylistTrackCreateRequest request = new PlaylistTrackCreateRequest(10L);
            PlaylistTrackResponse response = new PlaylistTrackResponse(
                    1L, 1L, 10L, 1, LocalDateTime.now()
            );

            given(playlistTrackService.addTrack(any(), any(), any(PlaylistTrackCreateRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/playlists/{playlistId}/tracks", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("플레이리스트 트랙 추가 성공"))
                    .andExpect(jsonPath("$.data.id").value(1L))
                    .andExpect(jsonPath("$.data.playlistId").value(1L))
                    .andExpect(jsonPath("$.data.trackId").value(10L))
                    .andExpect(jsonPath("$.data.position").value(1));
        }

        @Test
        @DisplayName("trackId가 없으면 400 반환")
        void addTrackValidationFail() throws Exception {
            // given
            String invalidRequest = """
                    {
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/playlists/{playlistId}/tracks", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("플레이리스트 권한이 없으면 403 반환")
        void addTrackForbidden() throws Exception {
            // given
            PlaylistTrackCreateRequest request = new PlaylistTrackCreateRequest(10L);

            given(playlistTrackService.addTrack(any(), eq(1L), any(PlaylistTrackCreateRequest.class)))
                    .willThrow(new BusinessException(PlaylistErrorCode.PLAYLIST_ACCESS_FORBIDDEN));

            // when & then
            mockMvc.perform(post("/api/playlists/{playlistId}/tracks", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message")
                            .value(PlaylistErrorCode.PLAYLIST_ACCESS_FORBIDDEN.getMessage()));
        }

        @Test
        @DisplayName("플레이리스트가 존재하지 않으면 404 반환")
        void addTrackPlaylistNotFound() throws Exception {
            // given
            PlaylistTrackCreateRequest request = new PlaylistTrackCreateRequest(10L);

            given(playlistTrackService.addTrack(any(), eq(1L), any(PlaylistTrackCreateRequest.class)))
                    .willThrow(new BusinessException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/api/playlists/{playlistId}/tracks", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value(PlaylistErrorCode.PLAYLIST_NOT_FOUND.getMessage()));
        }

        @Test
        @DisplayName("이미 추가된 트랙이면 409 반환")
        void addTrackDuplicate() throws Exception {
            // given
            PlaylistTrackCreateRequest request = new PlaylistTrackCreateRequest(10L);

            given(playlistTrackService.addTrack(any(), eq(1L), any(PlaylistTrackCreateRequest.class)))
                    .willThrow(new BusinessException(PlaylistErrorCode.PLAYLIST_TRACK_ALREADY_EXISTS));

            // when & then
            mockMvc.perform(post("/api/playlists/{playlistId}/tracks", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message")
                            .value(PlaylistErrorCode.PLAYLIST_TRACK_ALREADY_EXISTS.getMessage()));
        }
    }

    @Nested
    @DisplayName("플레이리스트 트랙 목록 조회")
    class GetTracks {

        @Test
        @DisplayName("플레이리스트 트랙 목록 조회 성공 시 200 반환")
        void getTracksSuccess() throws Exception {
            // given
            List<PlaylistTrackResponse> response = List.of(
                    new PlaylistTrackResponse(1L, 1L, 10L, 1, LocalDateTime.now()),
                    new PlaylistTrackResponse(2L, 1L, 20L, 2, LocalDateTime.now())
            );

            given(playlistTrackService.getTracks(any(), any()))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/playlists/{playlistId}/tracks", 1L))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("플레이리스트 트랙 목록 조회 성공"))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].trackId").value(10L))
                    .andExpect(jsonPath("$.data[0].position").value(1))
                    .andExpect(jsonPath("$.data[1].trackId").value(20L))
                    .andExpect(jsonPath("$.data[1].position").value(2));
        }

        @Test
        @DisplayName("플레이리스트가 존재하지 않으면 404 반환")
        void getTracksPlaylistNotFound() throws Exception {
            // given
            given(playlistTrackService.getTracks(any(), eq(1L)))
                    .willThrow(new BusinessException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/playlists/{playlistId}/tracks", 1L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value(PlaylistErrorCode.PLAYLIST_NOT_FOUND.getMessage()));
        }
    }

    @Nested
    @DisplayName("플레이리스트 트랙 순서 변경")
    class UpdateTrackOrder {

        @Test
        @DisplayName("플레이리스트 트랙 순서 변경 성공 시 200 반환")
        void updateTrackOrderSuccess() throws Exception {
            // given
            PlaylistTrackOrderUpdateRequest request = new PlaylistTrackOrderUpdateRequest(10L, 2);

            willDoNothing().given(playlistTrackService)
                    .updateTrackOrder(any(), any(), any(PlaylistTrackOrderUpdateRequest.class));

            // when & then
            mockMvc.perform(patch("/api/playlists/{playlistId}/tracks/index", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("플레이리스트 트랙 순서 변경 성공"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }

        @Test
        @DisplayName("position이 1 미만이면 400 반환")
        void updateTrackOrderValidationFail() throws Exception {
            // given
            PlaylistTrackOrderUpdateRequest request = new PlaylistTrackOrderUpdateRequest(10L, 0);

            // when & then
            mockMvc.perform(patch("/api/playlists/{playlistId}/tracks/index", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("플레이리스트 권한이 없으면 403 반환")
        void updateTrackOrderForbidden() throws Exception {
            // given
            PlaylistTrackOrderUpdateRequest request = new PlaylistTrackOrderUpdateRequest(10L, 2);

            willThrow(new BusinessException(PlaylistErrorCode.PLAYLIST_UPDATE_FORBIDDEN))
                    .given(playlistTrackService)
                    .updateTrackOrder(any(), eq(1L), any(PlaylistTrackOrderUpdateRequest.class));

            // when & then
            mockMvc.perform(patch("/api/playlists/{playlistId}/tracks/index", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message")
                            .value(PlaylistErrorCode.PLAYLIST_UPDATE_FORBIDDEN.getMessage()));
        }

        @Test
        @DisplayName("플레이리스트가 존재하지 않으면 404 반환")
        void updateTrackOrderPlaylistNotFound() throws Exception {
            // given
            PlaylistTrackOrderUpdateRequest request = new PlaylistTrackOrderUpdateRequest(10L, 2);

            willThrow(new BusinessException(PlaylistErrorCode.PLAYLIST_NOT_FOUND))
                    .given(playlistTrackService)
                    .updateTrackOrder(any(), eq(1L), any(PlaylistTrackOrderUpdateRequest.class));

            // when & then
            mockMvc.perform(patch("/api/playlists/{playlistId}/tracks/index", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value(PlaylistErrorCode.PLAYLIST_NOT_FOUND.getMessage()));
        }

        @Test
        @DisplayName("트랙 순서 충돌 시 409 반환")
        void updateTrackOrderConflict() throws Exception {
            // given
            PlaylistTrackOrderUpdateRequest request = new PlaylistTrackOrderUpdateRequest(10L, 2);

            willThrow(new BusinessException(PlaylistErrorCode.PLAYLIST_TRACK_POSITION_CONFLICT))
                    .given(playlistTrackService)
                    .updateTrackOrder(any(), eq(1L), any(PlaylistTrackOrderUpdateRequest.class));

            // when & then
            mockMvc.perform(patch("/api/playlists/{playlistId}/tracks/index", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message")
                            .value(PlaylistErrorCode.PLAYLIST_TRACK_POSITION_CONFLICT.getMessage()));
        }
    }

    @Nested
    @DisplayName("플레이리스트 트랙 삭제")
    class DeleteTrack {

        @Test
        @DisplayName("플레이리스트 트랙 삭제 성공 시 200 반환")
        void deleteTrackSuccess() throws Exception {
            // given
            willDoNothing().given(playlistTrackService)
                    .deleteTrack(any(), any(), any());

            // when & then
            mockMvc.perform(delete("/api/playlists/{playlistId}/tracks/{trackId}", 1L, 10L))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("플레이리스트 트랙 삭제 성공"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }

        @Test
        @DisplayName("플레이리스트 권한이 없으면 403 반환")
        void deleteTrackForbidden() throws Exception {
            // given
            willThrow(new BusinessException(PlaylistErrorCode.PLAYLIST_DELETE_FORBIDDEN))
                    .given(playlistTrackService)
                    .deleteTrack(any(), eq(1L), eq(10L));

            // when & then
            mockMvc.perform(delete("/api/playlists/{playlistId}/tracks/{trackId}", 1L, 10L))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message")
                            .value(PlaylistErrorCode.PLAYLIST_DELETE_FORBIDDEN.getMessage()));
        }

        @Test
        @DisplayName("삭제할 트랙이 존재하지 않으면 404 반환")
        void deleteTrackNotFound() throws Exception {
            // given
            willThrow(new BusinessException(PlaylistErrorCode.PLAYLIST_TRACK_NOT_FOUND))
                    .given(playlistTrackService)
                    .deleteTrack(any(), eq(1L), eq(10L));

            // when & then
            mockMvc.perform(delete("/api/playlists/{playlistId}/tracks/{trackId}", 1L, 10L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value(PlaylistErrorCode.PLAYLIST_TRACK_NOT_FOUND.getMessage()));
        }
    }
}
