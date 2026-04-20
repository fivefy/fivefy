package com.fivefy.domain.playlist.controller;

import com.fivefy.common.config.security.JwtUtil;
import com.fivefy.common.docs.RestDocsSupport;
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
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser
@WebMvcTest(PlaylistController.class)
class PlaylistControllerTest extends RestDocsSupport {

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
                            .with(csrf().asHeader())
                            .param("userId", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("플레이리스트 생성 성공"))
                    .andExpect(jsonPath("$.data.id").value(1L))
                    .andExpect(jsonPath("$.data.title").value("운동할 때 듣는 노래"))
                    .andExpect(jsonPath("$.data.description").value("신나는 음악 모음"))
                    .andDo(document("playlist-create",
                            requestFields(
                                    fieldWithPath("title").type(STRING).description("플레이리스트 제목"),
                                    fieldWithPath("description").type(STRING).description("플레이리스트 설명")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지"),
                                    fieldWithPath("data.id").type(NUMBER).description("플레이리스트 ID"),
                                    fieldWithPath("data.userId").type(NUMBER).description("유저 ID"),
                                    fieldWithPath("data.title").type(STRING).description("플레이리스트 제목"),
                                    fieldWithPath("data.description").type(STRING).description("플레이리스트 설명"),
                                    fieldWithPath("data.createdAt").type(STRING).description("생성 일시"),
                                    fieldWithPath("data.updatedAt").type(STRING).description("수정 일시"),
                                    fieldWithPath("data.deletedAt").type(NULL).description("삭제 일시")
                            )
                    ));
        }

        @Test
        @DisplayName("제목 없이 생성 요청 시 400 반환")
        void createPlaylistWithoutTitle() throws Exception {
            // given
            PlaylistCreateRequest request = new PlaylistCreateRequest("", "설명");

            // when & then
            mockMvc.perform(post("/api/playlists")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andDo(document("playlist-create-invalid",
                            requestFields(
                                    fieldWithPath("title").type(STRING).description("플레이리스트 제목 (값 없음)"),
                                    fieldWithPath("description").type(STRING).description("플레이리스트 설명")
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

        @Test
        @DisplayName("제목이 100자를 초과하면 400 반환")
        void createPlaylistWithTooLongTitle() throws Exception {
            // given
            String longTitle = "a".repeat(101);
            PlaylistCreateRequest request = new PlaylistCreateRequest(longTitle, "설명");

            // when & then
            mockMvc.perform(post("/api/playlists")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("유효한 구독이 없으면 403 반환")
        void createPlaylistWithoutValidSubscription() throws Exception {
            // given
            PlaylistCreateRequest request = new PlaylistCreateRequest("내 플레이리스트", "설명");

            given(playlistService.createPlaylist(any(), any(PlaylistCreateRequest.class)))
                    .willThrow(new BusinessException(PlaylistErrorCode.PLAYLIST_CREATION_SUBSCRIPTION_REQUIRED));

            // when & then
            mockMvc.perform(post("/api/playlists")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message")
                            .value(PlaylistErrorCode.PLAYLIST_CREATION_SUBSCRIPTION_REQUIRED.getMessage()))
                    .andDo(document("playlist-create-subscription-required",
                            requestFields(
                                    fieldWithPath("title").type(STRING).description("플레이리스트 제목"),
                                    fieldWithPath("description").type(STRING).description("플레이리스트 설명")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("에러 메시지")
                            )
                    ));
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
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(PlaylistErrorCode.DUPLICATE_PLAYLIST_NAME.getMessage()))
                    .andDo(document("playlist-create-duplicate",
                            requestFields(
                                    fieldWithPath("title").type(STRING).description("중복된 플레이리스트 제목"),
                                    fieldWithPath("description").type(STRING).description("플레이리스트 설명")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("에러 메시지")
                            )
                    ));
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
                    .andExpect(jsonPath("$.data.totalPages").value(1))
                    .andDo(document("playlist-get-list",
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지"),
                                    fieldWithPath("data.content").type(ARRAY).description("플레이리스트 목록"),
                                    fieldWithPath("data.content[].id").type(NUMBER).description("플레이리스트 ID"),
                                    fieldWithPath("data.content[].userId").type(NUMBER).description("유저 ID"),
                                    fieldWithPath("data.content[].title").type(STRING).description("플레이리스트 제목"),
                                    fieldWithPath("data.content[].description").type(STRING).description("플레이리스트 설명"),
                                    fieldWithPath("data.content[].createdAt").type(STRING).description("생성 일시"),
                                    fieldWithPath("data.content[].updatedAt").type(STRING).description("수정 일시"),
                                    fieldWithPath("data.content[].deletedAt").type(NULL).description("삭제 일시"),
                                    fieldWithPath("data.page").type(NUMBER).description("현재 페이지 번호"),
                                    fieldWithPath("data.size").type(NUMBER).description("페이지 크기"),
                                    fieldWithPath("data.totalElements").type(NUMBER).description("전체 데이터 수"),
                                    fieldWithPath("data.totalPages").type(NUMBER).description("전체 페이지 수")
                            )
                    ));
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
                    .andExpect(jsonPath("$.data.title").value("내 플레이리스트"))
                    .andDo(document("playlist-get",
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지"),
                                    fieldWithPath("data.id").type(NUMBER).description("플레이리스트 ID"),
                                    fieldWithPath("data.userId").type(NUMBER).description("유저 ID"),
                                    fieldWithPath("data.title").type(STRING).description("플레이리스트 제목"),
                                    fieldWithPath("data.description").type(STRING).description("플레이리스트 설명"),
                                    fieldWithPath("data.createdAt").type(STRING).description("생성 일시"),
                                    fieldWithPath("data.updatedAt").type(STRING).description("수정 일시"),
                                    fieldWithPath("data.deletedAt").type(NULL).description("삭제 일시")
                            )
                    ));
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
                    .andExpect(jsonPath("$.message").value(PlaylistErrorCode.PLAYLIST_NOT_FOUND.getMessage()))
                    .andDo(document("playlist-get-not-found",
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("에러 메시지")
                            )
                    ));
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
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("플레이리스트 수정 성공"))
                    .andExpect(jsonPath("$.data.title").value("수정된 제목"))
                    .andExpect(jsonPath("$.data.description").value("수정된 설명"))
                    .andDo(document("playlist-update",
                            requestFields(
                                    fieldWithPath("title").type(STRING).description("수정할 플레이리스트 제목"),
                                    fieldWithPath("description").type(STRING).description("수정할 플레이리스트 설명")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지"),
                                    fieldWithPath("data.id").type(NUMBER).description("플레이리스트 ID"),
                                    fieldWithPath("data.userId").type(NUMBER).description("유저 ID"),
                                    fieldWithPath("data.title").type(STRING).description("수정된 제목"),
                                    fieldWithPath("data.description").type(STRING).description("수정된 설명"),
                                    fieldWithPath("data.createdAt").type(STRING).description("생성 일시"),
                                    fieldWithPath("data.updatedAt").type(STRING).description("수정 일시"),
                                    fieldWithPath("data.deletedAt").type(NULL).description("삭제 일시")
                            )
                    ));
        }

        @Test
        @DisplayName("제목 없이 수정 요청 시 400 반환")
        void updatePlaylistWithoutTitle() throws Exception {
            // given
            PlaylistUpdateRequest request = new PlaylistUpdateRequest("", "설명");

            // when & then
            mockMvc.perform(patch("/api/playlists/1")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andDo(document("playlist-update-invalid",
                            requestFields(
                                    fieldWithPath("title").type(STRING).description("플레이리스트 제목 (값 없음)"),
                                    fieldWithPath("description").type(STRING).description("플레이리스트 설명")
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

        @Test
        @DisplayName("본인 소유가 아닌 플레이리스트 수정 시 403 반환")
        void updatePlaylistForbidden() throws Exception {
            // given
            PlaylistUpdateRequest request = new PlaylistUpdateRequest("수정 제목", "설명");

            given(playlistService.updatePlaylist(any(), eq(1L), any(PlaylistUpdateRequest.class)))
                    .willThrow(new BusinessException(PlaylistErrorCode.PLAYLIST_UPDATE_FORBIDDEN));

            // when & then
            mockMvc.perform(patch("/api/playlists/1")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(PlaylistErrorCode.PLAYLIST_UPDATE_FORBIDDEN.getMessage()))
                    .andDo(document("playlist-update-forbidden",
                            requestFields(
                                    fieldWithPath("title").type(STRING).description("수정할 플레이리스트 제목"),
                                    fieldWithPath("description").type(STRING).description("플레이리스트 설명")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("에러 메시지")
                            )
                    ));
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
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(PlaylistErrorCode.PLAYLIST_NOT_FOUND.getMessage()))
                    .andDo(document("playlist-update-not-found",
                            requestFields(
                                    fieldWithPath("title").type(STRING).description("수정할 플레이리스트 제목"),
                                    fieldWithPath("description").type(STRING).description("플레이리스트 설명")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("에러 메시지")
                            )
                    ));
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
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(PlaylistErrorCode.DUPLICATE_PLAYLIST_NAME.getMessage()))
                    .andDo(document("playlist-update-duplicate",
                            requestFields(
                                    fieldWithPath("title").type(STRING).description("중복된 플레이리스트 제목"),
                                    fieldWithPath("description").type(STRING).description("플레이리스트 설명")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("에러 메시지")
                            )
                    ));
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
            mockMvc.perform(delete("/api/playlists/1")
                            .with(csrf().asHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("플레이리스트 삭제 성공"))
                    .andExpect(jsonPath("$.data.id").value(1L))
                    .andDo(document("playlist-delete",
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지"),
                                    fieldWithPath("data.id").type(NUMBER).description("삭제된 플레이리스트 ID"),
                                    fieldWithPath("data.deletedAt").type(STRING).description("삭제 일시")
                            )
                    ));
        }

        @Test
        @DisplayName("본인 소유가 아닌 플레이리스트 삭제 시 403 반환")
        void deletePlaylistForbidden() throws Exception {
            // given
            given(playlistService.deletePlaylist(any(), eq(1L)))
                    .willThrow(new BusinessException(PlaylistErrorCode.PLAYLIST_DELETE_FORBIDDEN));

            // when & then
            mockMvc.perform(delete("/api/playlists/1")
                            .with(csrf().asHeader()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(PlaylistErrorCode.PLAYLIST_DELETE_FORBIDDEN.getMessage()))
                    .andDo(document("playlist-delete-forbidden",
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("에러 메시지")
                            )
                    ));
        }

        @Test
        @DisplayName("존재하지 않는 플레이리스트 삭제 시 404 반환")
        void deletePlaylistNotFound() throws Exception {
            // given
            given(playlistService.deletePlaylist(any(), eq(1L)))
                    .willThrow(new BusinessException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

            // when & then
            mockMvc.perform(delete("/api/playlists/1")
                            .with(csrf().asHeader()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(PlaylistErrorCode.PLAYLIST_NOT_FOUND.getMessage()))
                    .andDo(document("playlist-delete-not-found",
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("에러 메시지")
                            )
                    ));
        }
    }
}
