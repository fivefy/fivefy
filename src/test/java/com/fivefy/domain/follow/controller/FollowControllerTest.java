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
}
