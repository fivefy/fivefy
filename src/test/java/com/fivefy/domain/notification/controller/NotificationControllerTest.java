package com.fivefy.domain.notification.controller;

import com.fivefy.common.config.security.JwtUtil;
import com.fivefy.domain.notification.dto.response.NotificationGetResponse;
import com.fivefy.domain.notification.enums.NotificationChannel;
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
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
            given(notificationService.subscribe(any())).willReturn(new SseEmitter());

            // when & then
            mockMvc.perform(get("/api/notifications/subscribe")
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
}