package com.fivefy.domain.follow.controller;

import com.fivefy.common.config.security.JwtUtil;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.follow.dto.request.FollowCreateRequest;
import com.fivefy.domain.follow.dto.response.FollowCreateResponse;
import com.fivefy.domain.follow.dto.response.FollowGetResponse;
import com.fivefy.domain.follow.enums.FollowErrorCode;
import com.fivefy.domain.follow.service.FollowService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FollowController.class)
@AutoConfigureMockMvc(addFilters = false)
class FollowControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private FollowService followService;
    @MockitoBean private JwtUtil jwtUtil;

    private static final Long ARTIST_ID = 2L;

    @Nested
    @DisplayName("팔로우 등록")
    class CreateFollow {

        @Test
        @DisplayName("팔로우 등록 성공 시 201 반환")
        void createFollow_success() throws Exception {
            // given
            FollowCreateRequest request = new FollowCreateRequest(ARTIST_ID);
            FollowCreateResponse response = new FollowCreateResponse(1L, ARTIST_ID, true, LocalDateTime.now());

            given(followService.createFollow(any(), eq(ARTIST_ID))).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/follows")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("팔로우 등록 성공"))
                    .andExpect(jsonPath("$.data.artistId").value(ARTIST_ID));
        }

        @Test
        @DisplayName("artistId null 시 400 반환")
        void createFollow_invalidRequest() throws Exception {
            // given
            FollowCreateRequest request = new FollowCreateRequest(null);

            // when & then
            mockMvc.perform(post("/api/follows")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("중복 팔로우 시 409 반환")
        void createFollow_duplicate() throws Exception {
            // given
            FollowCreateRequest request = new FollowCreateRequest(ARTIST_ID);
            given(followService.createFollow(any(), eq(ARTIST_ID)))
                    .willThrow(new BusinessException(FollowErrorCode.ERR_FOLLOW_ALREADY_EXISTS));

            // when & then
            mockMvc.perform(post("/api/follows")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(FollowErrorCode.ERR_FOLLOW_ALREADY_EXISTS.getMessage()));
        }
    }

    @Nested
    @DisplayName("팔로우 목록 조회")
    class GetFollows {

        @Test
        @DisplayName("팔로우 목록 조회 성공 시 200 반환")
        void getFollows_success() throws Exception {
            // given
            FollowGetResponse followGetResponse = new FollowGetResponse(1L, ARTIST_ID, true);
            PageImpl<FollowGetResponse> page = new PageImpl<>(
                    List.of(followGetResponse), PageRequest.of(0, 20), 1
            );

            given(followService.getFollows(any(), any())).willReturn(page);

            // when & then
            mockMvc.perform(get("/api/follows")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("팔로우 목록 조회 성공"))
                    .andExpect(jsonPath("$.data.content[0].artistId").value(ARTIST_ID))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }
    }

    @Nested
    @DisplayName("팔로우 취소")
    class DeleteFollow {

        @Test
        @DisplayName("팔로우 취소 성공 시 200 반환")
        void deleteFollow_success() throws Exception {
            // given
            doNothing().when(followService).deleteFollow(any(), eq(ARTIST_ID));

            // when & then
            mockMvc.perform(delete("/api/follows/{artistId}", ARTIST_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("팔로우 취소 성공"));
        }

        @Test
        @DisplayName("존재하지 않는 팔로우 취소 시 404 반환")
        void deleteFollow_notFound() throws Exception {
            // given
            doThrow(new BusinessException(FollowErrorCode.ERR_FOLLOW_NOT_FOUND))
                    .when(followService).deleteFollow(any(), eq(ARTIST_ID));

            // when & then
            mockMvc.perform(delete("/api/follows/{artistId}", ARTIST_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(FollowErrorCode.ERR_FOLLOW_NOT_FOUND.getMessage()));
        }
    }

    @Nested
    @DisplayName("알림 설정 토글")
    class ToggleNotification {

        @Test
        @DisplayName("알림 설정 토글 성공 시 200 반환")
        void toggleNotification_success() throws Exception {
            // given
            doNothing().when(followService).toggleNotification(any(), eq(ARTIST_ID));

            // when & then
            mockMvc.perform(patch("/api/follows/{artistId}/notifications", ARTIST_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("알림 설정 변경 성공"));
        }

        @Test
        @DisplayName("존재하지 않는 팔로우 토글 시 404 반환")
        void toggleNotification_notFound() throws Exception {
            // given
            doThrow(new BusinessException(FollowErrorCode.ERR_FOLLOW_NOT_FOUND))
                    .when(followService).toggleNotification(any(), eq(ARTIST_ID));

            // when & then
            mockMvc.perform(patch("/api/follows/{artistId}/notifications", ARTIST_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(FollowErrorCode.ERR_FOLLOW_NOT_FOUND.getMessage()));
        }
    }
}
