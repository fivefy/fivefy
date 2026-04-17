package com.fivefy.domain.like.controller;

import com.fivefy.common.config.security.JwtUtil;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.like.dto.request.LikeCreateRequest;
import com.fivefy.domain.like.dto.response.LikeCreateResponse;
import com.fivefy.domain.like.dto.response.LikeGetResponse;
import com.fivefy.domain.like.enums.LikeErrorCode;
import com.fivefy.domain.like.enums.TargetType;
import com.fivefy.domain.like.service.LikeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LikeController.class)
@AutoConfigureMockMvc(addFilters = false)
class LikeControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private LikeService likeService;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private StringRedisTemplate stringRedisTemplate;

    private static final Long TRACK_ID = 2L;
    private static final Long LIKE_ID = 4L;

    @Nested
    @DisplayName("좋아요 등록")
    class CreateLike {

        @Test
        @DisplayName("좋아요 등록 성공 시 201 반환")
        void createLike_success() throws Exception {
            // given
            LikeCreateRequest request = new LikeCreateRequest(TRACK_ID, TargetType.TRACK);
            LikeCreateResponse response = new LikeCreateResponse(LIKE_ID, TRACK_ID, TargetType.TRACK, LocalDateTime.now());

            given(likeService.createLike(eq(TRACK_ID), eq(TargetType.TRACK), any())).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/likes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("좋아요 등록 성공"))
                    .andExpect(jsonPath("$.data.targetId").value(TRACK_ID))
                    .andExpect(jsonPath("$.data.targetType").value("TRACK"));
        }

        @Test
        @DisplayName("targetId null 시 400 반환")
        void createLike_targetIdNull() throws Exception {
            // given
            LikeCreateRequest request = new LikeCreateRequest(null, TargetType.TRACK);

            // when & then
            mockMvc.perform(post("/api/likes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("targetType null 시 400 반환")
        void createLike_targetTypeNull() throws Exception {
            // given
            LikeCreateRequest request = new LikeCreateRequest(TRACK_ID, null);

            // when & then
            mockMvc.perform(post("/api/likes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("중복 좋아요 시 409 반환")
        void createLike_duplicate() throws Exception {
            // given
            LikeCreateRequest request = new LikeCreateRequest(TRACK_ID, TargetType.TRACK);
            given(likeService.createLike(eq(TRACK_ID), eq(TargetType.TRACK), any()))
                    .willThrow(new BusinessException(LikeErrorCode.ERR_LIKE_ALREADY_EXISTS));

            // when & then
            mockMvc.perform(post("/api/likes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(LikeErrorCode.ERR_LIKE_ALREADY_EXISTS.getMessage()));
        }
    }

    @Nested
    @DisplayName("좋아요 목록 조회")
    class GetLikes {

        @Test
        @DisplayName("좋아요 목록 조회 성공 시 200 반환")
        void getLikes_success() throws Exception {
            // given
            LikeGetResponse likeGetResponse = new LikeGetResponse(LIKE_ID, TRACK_ID, TargetType.TRACK, "title", "artist", LocalDateTime.now());
            PageImpl<LikeGetResponse> page = new PageImpl<>(
                    List.of(likeGetResponse), PageRequest.of(0, 20), 1
            );

            given(likeService.getLikes(any(), any(), any())).willReturn(page);

            // when & then
            mockMvc.perform(get("/api/likes")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("좋아요 목록 조회 성공"))
                    .andExpect(jsonPath("$.data.content[0].targetId").value(TRACK_ID))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        @DisplayName("targetType 필터링 조회 성공 시 200 반환")
        void getLikes_withTargetTypeFilter() throws Exception {
            // given
            LikeGetResponse likeGetResponse = new LikeGetResponse(LIKE_ID, TRACK_ID, TargetType.TRACK, "title", "artist", LocalDateTime.now());
            PageImpl<LikeGetResponse> page = new PageImpl<>(
                    List.of(likeGetResponse), PageRequest.of(0, 20), 1
            );

            given(likeService.getLikes(any(), eq(TargetType.TRACK), any())).willReturn(page);

            // when & then
            mockMvc.perform(get("/api/likes")
                            .param("targetType", "TRACK")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].targetType").value("TRACK"));
        }
    }

    @Nested
    @DisplayName("좋아요 취소")
    class DeleteLike {

        @Test
        @DisplayName("좋아요 취소 성공 시 200 반환")
        void deleteLike_success() throws Exception {
            // given
            doNothing().when(likeService).deleteLike(any(), eq(LIKE_ID));

            // when & then
            mockMvc.perform(delete("/api/likes/{likeId}", LIKE_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("좋아요 취소 성공"));
        }

        @Test
        @DisplayName("존재하지 않는 좋아요 취소 시 404 반환")
        void deleteLike_notFound() throws Exception {
            // given
            doThrow(new BusinessException(LikeErrorCode.ERR_LIKE_NOT_FOUND))
                    .when(likeService).deleteLike(any(), eq(LIKE_ID));

            // when & then
            mockMvc.perform(delete("/api/likes/{likeId}", LIKE_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(LikeErrorCode.ERR_LIKE_NOT_FOUND.getMessage()));
        }
    }
}
