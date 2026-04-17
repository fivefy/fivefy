package com.fivefy.domain.playback.controller;

import com.fivefy.common.config.security.JwtUtil;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.playback.dto.request.PlaybackPauseRequest;
import com.fivefy.domain.playback.dto.request.PlaybackPlayRequest;
import com.fivefy.domain.playback.dto.request.PlaybackSkipRequest;
import com.fivefy.domain.playback.dto.request.PlaybackStopRequest;
import com.fivefy.domain.playback.dto.response.PlaybackResponse;
import com.fivefy.domain.playback.enums.PlaybackErrorCode;
import com.fivefy.domain.playback.enums.PlaybackStatus;
import com.fivefy.domain.playback.service.PlaybackService;
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
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlaybackController.class)
@AutoConfigureMockMvc(addFilters = false)
class PlaybackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PlaybackService playbackService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @Nested
    @DisplayName("재생 시작")
    class Play {

        @Test
        @DisplayName("재생 시작 성공 시 200 반환")
        void playSuccess() throws Exception {
            // given
            PlaybackPlayRequest request = new PlaybackPlayRequest(1L, 10L, "session-1", "device-1");
            PlaybackResponse response = new PlaybackResponse(
                    1L,
                    1L,
                    10L,
                    100L,
                    "session-1",
                    "device-1",
                    PlaybackStatus.PLAYING,
                    0,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    null
            );

            given(playbackService.play(any(), any(PlaybackPlayRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/playbacks/play")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("재생 시작 성공"))
                    .andExpect(jsonPath("$.data.id").value(1L))
                    .andExpect(jsonPath("$.data.playlistId").value(1L))
                    .andExpect(jsonPath("$.data.trackId").value(10L))
                    .andExpect(jsonPath("$.data.status").value("PLAYING"));
        }

        @Test
        @DisplayName("playlistId 없이 재생 시작 요청 시 400 반환")
        void playWithoutPlaylistId() throws Exception {
            // given
            String request = """
                    {
                      "trackId": 10,
                      "sessionId": "session-1",
                      "deviceId": "device-1"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/playbacks/play")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("trackId 없이 재생 시작 요청 시 400 반환")
        void playWithoutTrackId() throws Exception {
            // given
            String request = """
                    {
                      "playlistId": 1,
                      "sessionId": "session-1",
                      "deviceId": "device-1"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/playbacks/play")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("sessionId 없이 재생 시작 요청 시 400 반환")
        void playWithoutSessionId() throws Exception {
            // given
            String request = """
                    {
                      "playlistId": 1,
                      "trackId": 10,
                      "sessionId": "",
                      "deviceId": "device-1"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/playbacks/play")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("이미 재생 중인 곡을 다시 재생 요청하면 409 반환")
        void playInvalidState() throws Exception {
            // given
            PlaybackPlayRequest request = new PlaybackPlayRequest(1L, 10L, "session-1", "device-1");

            given(playbackService.play(any(), any(PlaybackPlayRequest.class)))
                    .willThrow(new BusinessException(PlaybackErrorCode.INVALID_PLAYBACK_STATE));

            // when & then
            mockMvc.perform(post("/api/playbacks/play")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(PlaybackErrorCode.INVALID_PLAYBACK_STATE.getMessage()));
        }

        @Test
        @DisplayName("플레이리스트에 포함되지 않은 트랙 재생 요청 시 400 반환")
        void playPlaylistTrackNotIncluded() throws Exception {
            // given
            PlaybackPlayRequest request = new PlaybackPlayRequest(1L, 10L, "session-1", "device-1");

            given(playbackService.play(any(), any(PlaybackPlayRequest.class)))
                    .willThrow(new BusinessException(PlaybackErrorCode.PLAYLIST_TRACK_NOT_INCLUDED));

            // when & then
            mockMvc.perform(post("/api/playbacks/play")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(PlaybackErrorCode.PLAYLIST_TRACK_NOT_INCLUDED.getMessage()));
        }
    }

    @Nested
    @DisplayName("재생 일시정지")
    class Pause {

        @Test
        @DisplayName("재생 일시정지 성공 시 200 반환")
        void pauseSuccess() throws Exception {
            // given
            PlaybackPauseRequest request = new PlaybackPauseRequest(1L);
            PlaybackResponse response = new PlaybackResponse(
                    1L,
                    1L,
                    10L,
                    100L,
                    "session-1",
                    "device-1",
                    PlaybackStatus.PAUSED,
                    30,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    null
            );

            given(playbackService.pause(any(), any(PlaybackPauseRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/playbacks/pause")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("재생 일시정지 성공"))
                    .andExpect(jsonPath("$.data.status").value("PAUSED"));
        }

        @Test
        @DisplayName("id 없이 일시정지 요청 시 400 반환")
        void pauseWithoutId() throws Exception {
            // given
            String request = "{}";

            // when & then
            mockMvc.perform(post("/api/playbacks/pause")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("존재하지 않는 재생 기록 일시정지 시 404 반환")
        void pauseNotFound() throws Exception {
            // given
            PlaybackPauseRequest request = new PlaybackPauseRequest(1L);

            given(playbackService.pause(any(), any(PlaybackPauseRequest.class)))
                    .willThrow(new BusinessException(PlaybackErrorCode.PLAYBACK_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/api/playbacks/pause")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(PlaybackErrorCode.PLAYBACK_NOT_FOUND.getMessage()));
        }

        @Test
        @DisplayName("현재 재생 중이 아닌 경우 일시정지 시 409 반환")
        void pauseCurrentPlaybackNotFound() throws Exception {
            // given
            PlaybackPauseRequest request = new PlaybackPauseRequest(1L);

            given(playbackService.pause(any(), any(PlaybackPauseRequest.class)))
                    .willThrow(new BusinessException(PlaybackErrorCode.CURRENT_PLAYBACK_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/api/playbacks/pause")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(PlaybackErrorCode.CURRENT_PLAYBACK_NOT_FOUND.getMessage()));
        }
    }

    @Nested
    @DisplayName("곡 건너뛰기")
    class Skip {

        @Test
        @DisplayName("곡 건너뛰기 성공 시 200 반환")
        void skipSuccess() throws Exception {
            // given
            PlaybackSkipRequest request = new PlaybackSkipRequest(1L);
            PlaybackResponse response = new PlaybackResponse(
                    2L,
                    1L,
                    20L,
                    100L,
                    "session-1",
                    "device-1",
                    PlaybackStatus.PLAYING,
                    0,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    null
            );

            given(playbackService.skip(any(), any(PlaybackSkipRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/playbacks/skip")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("곡 건너뛰기 성공"))
                    .andExpect(jsonPath("$.data.id").value(2L))
                    .andExpect(jsonPath("$.data.trackId").value(20L))
                    .andExpect(jsonPath("$.data.status").value("PLAYING"));
        }

        @Test
        @DisplayName("id 없이 곡 건너뛰기 요청 시 400 반환")
        void skipWithoutId() throws Exception {
            // given
            String request = "{}";

            // when & then
            mockMvc.perform(post("/api/playbacks/skip")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("존재하지 않는 재생 기록 건너뛰기 시 404 반환")
        void skipNotFound() throws Exception {
            // given
            PlaybackSkipRequest request = new PlaybackSkipRequest(1L);

            given(playbackService.skip(any(), any(PlaybackSkipRequest.class)))
                    .willThrow(new BusinessException(PlaybackErrorCode.PLAYBACK_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/api/playbacks/skip")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(PlaybackErrorCode.PLAYBACK_NOT_FOUND.getMessage()));
        }

        @Test
        @DisplayName("현재 재생 상태에서 건너뛸 수 없으면 409 반환")
        void skipInvalidState() throws Exception {
            // given
            PlaybackSkipRequest request = new PlaybackSkipRequest(1L);

            given(playbackService.skip(any(), any(PlaybackSkipRequest.class)))
                    .willThrow(new BusinessException(PlaybackErrorCode.INVALID_PLAYBACK_STATE));

            // when & then
            mockMvc.perform(post("/api/playbacks/skip")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(PlaybackErrorCode.INVALID_PLAYBACK_STATE.getMessage()));
        }

        @Test
        @DisplayName("플레이리스트 트랙 정보가 없으면 404 반환")
        void skipPlaylistTrackNotFound() throws Exception {
            // given
            PlaybackSkipRequest request = new PlaybackSkipRequest(1L);

            given(playbackService.skip(any(), any(PlaybackSkipRequest.class)))
                    .willThrow(new BusinessException(PlaybackErrorCode.PLAYLIST_TRACK_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/api/playbacks/skip")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(PlaybackErrorCode.PLAYLIST_TRACK_NOT_FOUND.getMessage()));
        }

        @Test
        @DisplayName("현재 재생 트랙 정보와 플레이리스트 트랙 순서가 일치하지 않으면 409 반환")
        void skipTrackMismatch() throws Exception {
            // given
            PlaybackSkipRequest request = new PlaybackSkipRequest(1L);

            given(playbackService.skip(any(), any(PlaybackSkipRequest.class)))
                    .willThrow(new BusinessException(PlaybackErrorCode.PLAYBACK_TRACK_MISMATCH));

            // when & then
            mockMvc.perform(post("/api/playbacks/skip")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(PlaybackErrorCode.PLAYBACK_TRACK_MISMATCH.getMessage()));
        }
    }

    @Nested
    @DisplayName("곡 정지")
    class Stop {

        @Test
        @DisplayName("곡 정지 성공 시 200 반환")
        void stopSuccess() throws Exception {
            // given
            PlaybackStopRequest request = new PlaybackStopRequest(1L);
            PlaybackResponse response = new PlaybackResponse(
                    1L,
                    1L,
                    10L,
                    100L,
                    "session-1",
                    "device-1",
                    PlaybackStatus.STOPPED,
                    40,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );

            given(playbackService.stop(any(), any(PlaybackStopRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/playbacks/stop")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("곡 정지 성공"))
                    .andExpect(jsonPath("$.data.status").value("STOPPED"));
        }

        @Test
        @DisplayName("id 없이 곡 정지 요청 시 400 반환")
        void stopWithoutId() throws Exception {
            // given
            String request = "{}";

            // when & then
            mockMvc.perform(post("/api/playbacks/stop")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("존재하지 않는 재생 기록 정지 시 404 반환")
        void stopNotFound() throws Exception {
            // given
            PlaybackStopRequest request = new PlaybackStopRequest(1L);

            given(playbackService.stop(any(), any(PlaybackStopRequest.class)))
                    .willThrow(new BusinessException(PlaybackErrorCode.PLAYBACK_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/api/playbacks/stop")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(PlaybackErrorCode.PLAYBACK_NOT_FOUND.getMessage()));
        }

        @Test
        @DisplayName("현재 재생 상태에서 정지할 수 없으면 409 반환")
        void stopInvalidState() throws Exception {
            // given
            PlaybackStopRequest request = new PlaybackStopRequest(1L);

            given(playbackService.stop(any(), any(PlaybackStopRequest.class)))
                    .willThrow(new BusinessException(PlaybackErrorCode.INVALID_PLAYBACK_STATE));

            // when & then
            mockMvc.perform(post("/api/playbacks/stop")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(PlaybackErrorCode.INVALID_PLAYBACK_STATE.getMessage()));
        }
    }

    @Nested
    @DisplayName("재생 기록 조회")
    class GetPlaybackHistory {

        @Test
        @DisplayName("재생 기록 조회 성공 시 200 반환")
        void getPlaybackHistorySuccess() throws Exception {
            // given
            PlaybackResponse response1 = new PlaybackResponse(
                    2L,
                    1L,
                    20L,
                    100L,
                    "session-1",
                    "device-1",
                    PlaybackStatus.SKIPPED,
                    25,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
            PlaybackResponse response2 = new PlaybackResponse(
                    1L,
                    1L,
                    10L,
                    100L,
                    "session-1",
                    "device-1",
                    PlaybackStatus.COMPLETED,
                    180,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );

            given(playbackService.getPlaybackHistory(any()))
                    .willReturn(List.of(response1, response2));

            // when & then
            mockMvc.perform(get("/api/me/playback-history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("재생 기록 조회 성공"))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].id").value(2L))
                    .andExpect(jsonPath("$.data[0].status").value("SKIPPED"))
                    .andExpect(jsonPath("$.data[1].id").value(1L))
                    .andExpect(jsonPath("$.data[1].status").value("COMPLETED"));
        }
    }
}
