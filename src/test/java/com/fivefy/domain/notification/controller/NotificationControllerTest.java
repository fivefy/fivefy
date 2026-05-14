package com.fivefy.domain.notification.controller;

import com.fivefy.common.config.security.JwtUtil;
import com.fivefy.common.docs.RestDocsSupport;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.notification.dto.response.NotificationGetResponse;
import com.fivefy.domain.notification.enums.NotificationChannel;
import com.fivefy.domain.notification.enums.NotificationErrorCode;
import com.fivefy.domain.notification.enums.NotificationStatus;
import com.fivefy.domain.notification.enums.NotificationType;
import com.fivefy.domain.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * NotificationController 웹 계층 테스트 및 REST Docs 문서화
 */
@WebMvcTest(NotificationController.class)
class NotificationControllerTest extends RestDocsSupport {

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    private static final Long NOTIFICATION_ID = 10L;

    private NotificationGetResponse makeResponse() {
        return new NotificationGetResponse(
                NOTIFICATION_ID,
                NotificationType.NEW_FOLLOWER,
                "새 팔로워가 생겼습니다",
                NotificationStatus.SENT,
                NotificationChannel.IN_APP,
                LocalDateTime.of(2026, 5, 13, 9, 0, 0),
                LocalDateTime.of(2026, 5, 13, 10, 0, 0)
        );
    }

    @Nested
    @WithMockUser
    @DisplayName("SSE 구독 API")
    class Subscribe {

        @Test
        @DisplayName("구독 요청 시 200 반환")
        void subscribe_success() throws Exception {
            given(notificationService.subscribe(any(), any())).willReturn(new SseEmitter());

            mockMvc.perform(get("/api/notifications/subscribe")
                            .accept(MediaType.TEXT_EVENT_STREAM))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                    .andDo(document("notification-subscribe",
                            requestHeaders(
                                    headerWithName("Last-Event-ID")
                                            .description("재연결 시 마지막 이벤트 ID (선택)").optional()
                            )
                    ));
        }

        @Test
        @DisplayName("Last-Event-ID 헤더 재연결 시 200 반환")
        void subscribe_withLastEventId() throws Exception {
            given(notificationService.subscribe(any(), eq(5L))).willReturn(new SseEmitter());

            mockMvc.perform(get("/api/notifications/subscribe")
                            .accept(MediaType.TEXT_EVENT_STREAM)
                            .header("Last-Event-ID", "5"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @WithMockUser
    @DisplayName("알림 목록 조회 API")
    class GetAllNotification {

        @Test
        @DisplayName("알림 목록 조회 성공 시 200 반환")
        void getAllNotification_success() throws Exception {
            Page<NotificationGetResponse> page = new PageImpl<>(
                    List.of(makeResponse()), PageRequest.of(0, 20), 1
            );
            given(notificationService.getNotifications(any(), any())).willReturn(page);

            mockMvc.perform(get("/api/notifications")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("알림 목록 조회 성공"))
                    .andExpect(jsonPath("$.data.content[0].id").value(NOTIFICATION_ID))
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andDo(document("notification-list",
                            queryParameters(
                                    parameterWithName("page")
                                            .description("페이지 번호 (0부터 시작)").optional(),
                                    parameterWithName("size")
                                            .description("페이지 크기 (기본 20)").optional(),
                                    parameterWithName("sort")
                                            .description("정렬 조건 (기본 createdAt,asc)").optional()
                            ),
                            responseFields(
                                    baseSuccessResponseFields()
                            ).and(
                                    pageResponseFields()
                            ).and(
                                    notificationListContentResponseFields()
                            )
                    ));
        }
    }

    @Nested
    @WithMockUser
    @DisplayName("읽지 않은 알림 수 조회 API")
    class GetUnreadCount {

        @Test
        @DisplayName("읽지 않은 알림 수 조회 성공 시 200 반환")
        void getUnreadCount_success() throws Exception {
            given(notificationService.getUnreadCount(any())).willReturn(7L);

            mockMvc.perform(get("/api/notifications/unread-count"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("읽지 않은 알림 수 조회 성공"))
                    .andExpect(jsonPath("$.data").value(7))
                    .andDo(document("notification-unread-count",
                            responseFields(
                                    baseSuccessResponseFields()
                            ).and(
                                    fieldWithPath("data").type(NUMBER).description("읽지 않은 알림 수")
                            )
                    ));
        }
    }

    @Nested
    @WithMockUser
    @DisplayName("단건 읽음 처리 API")
    class MarkAsRead {

        @Test
        @DisplayName("단건 읽음 처리 성공 시 200 반환")
        void markAsRead_success() throws Exception {
            doNothing().when(notificationService).markAsRead(any(), eq(NOTIFICATION_ID));

            mockMvc.perform(patch("/api/notifications/{notificationId}/read", NOTIFICATION_ID)
                            .with(csrf().asHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("알림 읽음 처리 성공"))
                    .andDo(document("notification-read",
                            pathParameters(
                                    parameterWithName("notificationId").description("읽음 처리할 알림 ID")
                            ),
                            responseFields(
                                    baseSuccessResponseFields()
                            )
                    ));
        }

        @Test
        @DisplayName("존재하지 않는 알림 읽음 처리 시 404 반환")
        void markAsRead_notFound() throws Exception {
            doThrow(new BusinessException(NotificationErrorCode.ERR_NOTIFICATION_NOT_FOUND))
                    .when(notificationService).markAsRead(any(), eq(NOTIFICATION_ID));

            mockMvc.perform(patch("/api/notifications/{notificationId}/read", NOTIFICATION_ID)
                            .with(csrf().asHeader()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(
                            NotificationErrorCode.ERR_NOTIFICATION_NOT_FOUND.getMessage()))
                    .andDo(document("notification-read-not-found",
                            pathParameters(
                                    parameterWithName("notificationId").description("읽음 처리할 알림 ID")
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            )
                    ));
        }

        @Test
        @DisplayName("다른 유저의 알림 읽음 처리 시 403 반환")
        void markAsRead_unauthorized() throws Exception {
            doThrow(new BusinessException(NotificationErrorCode.ERR_NOTIFICATION_UNAUTHORIZED))
                    .when(notificationService).markAsRead(any(), eq(NOTIFICATION_ID));

            mockMvc.perform(patch("/api/notifications/{notificationId}/read", NOTIFICATION_ID)
                            .with(csrf().asHeader()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(
                            NotificationErrorCode.ERR_NOTIFICATION_UNAUTHORIZED.getMessage()))
                    .andDo(document("notification-read-unauthorized",
                            pathParameters(
                                    parameterWithName("notificationId").description("읽음 처리할 알림 ID")
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            )
                    ));
        }
    }

    @Nested
    @WithMockUser
    @DisplayName("전체 읽음 처리 API")
    class MarkAllAsRead {

        @Test
        @DisplayName("전체 읽음 처리 성공 시 200 반환")
        void markAllAsRead_success() throws Exception {
            doNothing().when(notificationService).markAllAsRead(any());

            mockMvc.perform(patch("/api/notifications/read-all")
                            .with(csrf().asHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("전체 알림 읽음 처리 성공"))
                    .andDo(document("notification-read-all",
                            responseFields(
                                    baseSuccessResponseFields()
                            )
                    ));
        }
    }

    @Nested
    @WithMockUser
    @DisplayName("단건 알림 삭제 API")
    class DeleteNotification {

        @Test
        @DisplayName("단건 삭제 성공 시 200 반환")
        void deleteNotification_success() throws Exception {
            doNothing().when(notificationService).deleteNotification(any(), eq(NOTIFICATION_ID));

            mockMvc.perform(delete("/api/notifications/{notificationId}", NOTIFICATION_ID)
                            .with(csrf().asHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("알림 삭제 성공"))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andDo(document("notification-delete",
                            pathParameters(
                                    parameterWithName("notificationId").description("삭제할 알림 ID")
                            ),
                            responseFields(
                                    baseSuccessResponseFields()
                            )
                    ));
        }

        @Test
        @DisplayName("존재하지 않는 알림 삭제 시 404 반환")
        void deleteNotification_notFound() throws Exception {
            doThrow(new BusinessException(NotificationErrorCode.ERR_NOTIFICATION_NOT_FOUND))
                    .when(notificationService).deleteNotification(any(), eq(NOTIFICATION_ID));

            mockMvc.perform(delete("/api/notifications/{notificationId}", NOTIFICATION_ID)
                            .with(csrf().asHeader()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(
                            NotificationErrorCode.ERR_NOTIFICATION_NOT_FOUND.getMessage()))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andDo(document("notification-delete-not-found",
                            pathParameters(
                                    parameterWithName("notificationId").description("삭제할 알림 ID")
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            )
                    ));
        }

        @Test
        @DisplayName("다른 유저의 알림 삭제 시 403 반환")
        void deleteNotification_unauthorized() throws Exception {
            doThrow(new BusinessException(NotificationErrorCode.ERR_NOTIFICATION_UNAUTHORIZED))
                    .when(notificationService).deleteNotification(any(), eq(NOTIFICATION_ID));

            mockMvc.perform(delete("/api/notifications/{notificationId}", NOTIFICATION_ID)
                            .with(csrf().asHeader()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(
                            NotificationErrorCode.ERR_NOTIFICATION_UNAUTHORIZED.getMessage()))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andDo(document("notification-delete-unauthorized",
                            pathParameters(
                                    parameterWithName("notificationId").description("삭제할 알림 ID")
                            ),
                            responseFields(
                                    baseErrorResponseFields()
                            )
                    ));
        }
    }

    @Nested
    @WithMockUser
    @DisplayName("전체 알림 삭제 API")
    class DeleteAllNotifications {

        @Test
        @DisplayName("전체 삭제 성공 시 200 반환")
        void deleteAllNotifications_success() throws Exception {
            doNothing().when(notificationService).deleteAllNotifications(any());

            mockMvc.perform(delete("/api/notifications")
                            .with(csrf().asHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("전체 알림 삭제 성공"))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andDo(document("notification-delete-all",
                            responseFields(
                                    baseSuccessResponseFields()
                            )
                    ));
        }
    }

    // ===== RestDocs helper methods =====

    private FieldDescriptor[] baseSuccessResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                fieldWithPath("message").type(STRING).description("응답 메시지")
        };
    }

    private FieldDescriptor[] baseErrorResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                fieldWithPath("message").type(STRING).description("에러 메시지")
        };
    }

    private FieldDescriptor[] pageResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("data.page").type(NUMBER).description("현재 페이지"),
                fieldWithPath("data.size").type(NUMBER).description("페이지 크기"),
                fieldWithPath("data.totalElements").type(NUMBER).description("전체 데이터 수"),
                fieldWithPath("data.totalPages").type(NUMBER).description("전체 페이지 수")
        };
    }

    private FieldDescriptor[] notificationListContentResponseFields() {
        return new FieldDescriptor[]{
                fieldWithPath("data.content").type(ARRAY).description("알림 목록"),
                fieldWithPath("data.content[].id").type(NUMBER).description("알림 ID"),
                fieldWithPath("data.content[].type").type(STRING).description("알림 타입"),
                fieldWithPath("data.content[].content").type(STRING).description("알림 내용"),
                fieldWithPath("data.content[].status").type(STRING).description("알림 상태 (QUEUED/SENT/FAILED)"),
                fieldWithPath("data.content[].channel").type(STRING).description("알림 채널 (PUSH/EMAIL/IN_APP)"),
                fieldWithPath("data.content[].readAt").type(STRING).description("읽은 시각"),
                fieldWithPath("data.content[].createdAt").type(STRING).description("알림 생성 시각")
        };
    }
}
