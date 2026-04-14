package com.fivefy.domain.playlisttrack.controller;

import com.fivefy.common.config.security.JwtUtil;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
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
    }
}
