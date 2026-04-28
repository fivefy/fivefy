package com.fivefy.domain.notification.controller;

import com.fivefy.common.config.security.JwtUtil;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    private static final Long USER_ID = 1L;
    private static final Long NOTIFICATION_ID = 10L;

    private NotificationGetResponse makeResponse() {
        return new NotificationGetResponse(
                NOTIFICATION_ID,
                NotificationType.NEW_FOLLOWER,
                "테스트 알림",
                NotificationStatus.SENT,
                NotificationChannel.IN_APP,
                null,
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("SSE 구독")
    class Subscribe {

        @Test
        @DisplayName("구독 요청 시 200과 text/event-stream을 반환한다")
        void subscribe_returns200() throws Exception {
            // given
            given(notificationService.subscribe(any(), any())).willReturn(new SseEmitter());

            // when & then
            mockMvc.perform(get("/api/notifications/subscribe")
                            .accept(MediaType.TEXT_EVENT_STREAM))
                            .andExpect(status().isOk())
                            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
        }

        @Test
        @DisplayName("Last-Event-ID 헤더와 함께 재연결 시 200을 반환한다")
        void subscribe_withLastEventId_returns200() throws Exception {
            given(notificationService.subscribe(any(), eq(5L))).willReturn(new SseEmitter());

            mockMvc.perform(get("/api/notifications/subscribe")
                            .header("Last-Event-ID", "5")
                            .accept(MediaType.TEXT_EVENT_STREAM))
                            .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("알림 목록 조회")
    class GetAllNotification {

        @Test
        @DisplayName("알림 목록 조회 성공 시 200을 반환한다")
        void getAllNotification_returns200() throws Exception {
            // given
            Page<NotificationGetResponse> page = new PageImpl<>(
                    List.of(makeResponse()), PageRequest.of(0, 20), 1);
            given(notificationService.getNotifications(any(), any())).willReturn(page);

            // when & then
            mockMvc.perform(get("/api/notifications"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("알림 목록 조회 성공"))
                    .andExpect(jsonPath("$.data.content[0].id").value(NOTIFICATION_ID))
                    .andExpect(jsonPath("$.data.content[0].type").value("NEW_FOLLOWER"))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        @DisplayName("알림이 없으면 빈 목록을 반환한다")
        void getAllNotification_empty_returnsEmptyList() throws Exception {
            // given
            given(notificationService.getNotifications(any(), any())).willReturn(Page.empty());

            // when & then
            mockMvc.perform(get("/api/notifications"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isEmpty());
        }
    }

    @Nested
    @DisplayName("읽지 않은 알림 수 조회")
    class GetUnreadCount {

        @Test
        @DisplayName("읽지 않은 알림 수 조회 성공 시 200을 반환한다")
        void getUnreadCount_returns200() throws Exception {
            // given
            given(notificationService.getUnreadCount(any())).willReturn(5L);

            // when & then
            mockMvc.perform(get("/api/notifications/unread-count"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("읽지 않은 알림 수 조회 성공"))
                    .andExpect(jsonPath("$.data").value(5));
        }
    }

    @Nested
    @DisplayName("단건 읽음 처리")
    class MarkAsRead {

        @Test
        @DisplayName("읽음 처리 성공 시 200을 반환한다")
        void markAsRead_returns200() throws Exception {
            // given
            doNothing().when(notificationService).markAsRead(any(), eq(NOTIFICATION_ID));

            // when & then
            mockMvc.perform(patch("/api/notifications/{notificationId}/read", NOTIFICATION_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("알림 읽음 처리 성공"));
        }

        @Test
        @DisplayName("존재하지 않는 알림 읽음 처리 시 404를 반환한다")
        void markAsRead_notFound_returns404() throws Exception {
            // given
            doThrow(new BusinessException(NotificationErrorCode.ERR_NOTIFICATION_NOT_FOUND))
                    .when(notificationService).markAsRead(any(), eq(NOTIFICATION_ID));

            // when & then
            mockMvc.perform(patch("/api/notifications/{notificationId}/read", NOTIFICATION_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(NotificationErrorCode.ERR_NOTIFICATION_NOT_FOUND.getMessage()));
        }

        @Test
        @DisplayName("다른 유저의 알림 읽음 처리 시 403을 반환한다")
        void markAsRead_unauthorized_returns403() throws Exception {
            // given
            doThrow(new BusinessException(NotificationErrorCode.ERR_NOTIFICATION_UNAUTHORIZED))
                    .when(notificationService).markAsRead(any(), eq(NOTIFICATION_ID));

            // when & then
            mockMvc.perform(patch("/api/notifications/{notificationId}/read", NOTIFICATION_ID))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(NotificationErrorCode.ERR_NOTIFICATION_UNAUTHORIZED.getMessage()));
        }
    }

    @Nested
    @DisplayName("전체 읽음 처리")
    class MarkAllAsRead {

        @Test
        @DisplayName("전체 읽음 처리 성공 시 200을 반환한다")
        void markAllAsRead_returns200() throws Exception {
            // given
            doNothing().when(notificationService).markAllAsRead(any());

            // when & then
            mockMvc.perform(patch("/api/notifications/read-all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("전체 알림 읽음 처리 성공"));
        }
    }

    @Nested
    @DisplayName("단건 알림 삭제")
    class DeleteNotification {

        @Test
        @DisplayName("단건 삭제 성공 시 200을 반환한다")
        void deleteNotification_returns200() throws Exception {
            // given
            doNothing().when(notificationService).deleteNotification(any(), eq(NOTIFICATION_ID));

            // when & then
            mockMvc.perform(delete("/api/notifications/{notificationId}", NOTIFICATION_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("알림 삭제 성공"));
        }

        @Test
        @DisplayName("존재하지 않는 알림 삭제 시 404를 반환한다")
        void deleteNotification_notFound_returns404() throws Exception {
            // given
            doThrow(new BusinessException(NotificationErrorCode.ERR_NOTIFICATION_NOT_FOUND))
                    .when(notificationService).deleteNotification(any(), eq(NOTIFICATION_ID));

            // when & then
            mockMvc.perform(delete("/api/notifications/{notificationId}", NOTIFICATION_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(NotificationErrorCode.ERR_NOTIFICATION_NOT_FOUND.getMessage()));
        }

        @Test
        @DisplayName("다른 유저의 알림 삭제 시 403을 반환한다")
        void deleteNotification_unauthorized_returns403() throws Exception {
            // given
            doThrow(new BusinessException(NotificationErrorCode.ERR_NOTIFICATION_UNAUTHORIZED))
                    .when(notificationService).deleteNotification(any(), eq(NOTIFICATION_ID));

            // when & then
            mockMvc.perform(delete("/api/notifications/{notificationId}", NOTIFICATION_ID))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(NotificationErrorCode.ERR_NOTIFICATION_UNAUTHORIZED.getMessage()));
        }
    }

    @Nested
    @DisplayName("전체 알림 삭제")
    class DeleteAllNotifications {

        @Test
        @DisplayName("전체 삭제 성공 시 200을 반환한다")
        void deleteAllNotifications_returns200() throws Exception {
            // given
            doNothing().when(notificationService).deleteAllNotifications(any());

            // when & then
            mockMvc.perform(delete("/api/notifications"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("전체 알림 삭제 성공"));
        }
    }
}