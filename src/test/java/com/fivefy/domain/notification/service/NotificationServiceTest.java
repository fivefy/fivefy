package com.fivefy.domain.notification.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.notification.dto.response.NotificationGetResponse;
import com.fivefy.domain.notification.entity.Notification;
import com.fivefy.domain.notification.enums.NotificationChannel;
import com.fivefy.domain.notification.enums.NotificationErrorCode;
import com.fivefy.domain.notification.enums.NotificationStatus;
import com.fivefy.domain.notification.enums.NotificationType;
import com.fivefy.domain.notification.repository.NotificationRepository;
import com.fivefy.domain.notification.repository.SseEmitterRepository;
import com.fivefy.domain.user.entity.User;
import com.fivefy.domain.user.enums.UserErrorCode;
import com.fivefy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SseEmitterRepository sseEmitterRepository;

    @InjectMocks
    private NotificationService notificationService;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long NOTIFICATION_ID = 10L;

    private Notification makeNotification(Long userId) {
        return Notification.create(userId, "테스트 알림", NotificationType.NEW_FOLLOWER, NotificationChannel.IN_APP);
    }

    @Nested
    @DisplayName("SSE 구독")
    class Subscribe {

        @Test
        @DisplayName("구독 시 SseEmitter를 저장하고 초기 이벤트를 전송한다")
        void subscribe_savesEmitterAndSendsConnectEvent() throws Exception {
            // given
            given(notificationRepository.countByUserIdAndReadAtIsNull(USER_ID)).willReturn(3L);

            // when
            SseEmitter emitter = notificationService.subscribe(USER_ID);

            // then
            assertThat(emitter).isNotNull();
            verify(sseEmitterRepository).save(eq(USER_ID), any(SseEmitter.class));
            verify(notificationRepository).countByUserIdAndReadAtIsNull(USER_ID);
        }
    }

    @Nested
    @DisplayName("알림 발송")
    class Send {

        @Test
        @DisplayName("SSE 미연결 상태에서 알림 저장 시 QUEUED 상태를 유지한다")
        void send_noConnection_savesAsQueued() {
            // given
            Notification notification = makeNotification(USER_ID);
            given(notificationRepository.save(any())).willReturn(notification);
            given(sseEmitterRepository.findAllByUserId(USER_ID)).willReturn(List.of());

            // when
            notificationService.send(USER_ID, NotificationType.NEW_FOLLOWER, "테스트 알림");

            // then
            verify(notificationRepository, times(1)).save(any()); // 첫 저장만 호출
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.QUEUED);
        }

        @Test
        @DisplayName("SSE 연결된 상태에서 전송 성공 시 SENT 상태로 저장된다")
        void send_withConnection_marksAsSent() throws Exception {
            // given
            Notification notification = makeNotification(USER_ID);
            SseEmitter mockEmitter = mock(SseEmitter.class);

            given(notificationRepository.save(any())).willReturn(notification);
            given(sseEmitterRepository.findAllByUserId(USER_ID)).willReturn(List.of(mockEmitter));

            // when
            notificationService.send(USER_ID, NotificationType.NEW_FOLLOWER, "테스트 알림");

            // then
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
            verify(notificationRepository, times(2)).save(any()); // 첫 저장 + SENT 업데이트
        }

        @Test
        @DisplayName("SSE 전송 실패 시 FAILED 상태로 저장하고 emitter를 제거한다")
        void send_ioException_marksAsFailedAndDeletesEmitter() throws Exception {
            // given
            Notification notification = makeNotification(USER_ID);
            SseEmitter mockEmitter = mock(SseEmitter.class);

            given(notificationRepository.save(any())).willReturn(notification);
            given(sseEmitterRepository.findAllByUserId(USER_ID)).willReturn(List.of(mockEmitter));
            doThrow(new IOException("전송 실패")).when(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));

            // when
            notificationService.send(USER_ID, NotificationType.NEW_FOLLOWER, "테스트 알림");

            // then
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
            verify(notificationRepository, times(2)).save(any()); // 첫 저장 + FAILED 업데이트
            verify(sseEmitterRepository).delete(eq(USER_ID), eq(mockEmitter));
        }
    }

    @Nested
    @DisplayName("알림 목록 조회")
    class GetNotifications {

        @Test
        @DisplayName("알림 목록을 페이징하여 반환한다")
        void getNotifications_returnsPage() {
            // given
            User mockUser = mock(User.class);
            given(mockUser.getId()).willReturn(USER_ID);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(mockUser));

            Notification notification = makeNotification(USER_ID);
            Pageable pageable = PageRequest.of(0, 20);
            Page<Notification> page = new PageImpl<>(List.of(notification), pageable, 1);
            given(notificationRepository.findAllByUserId(USER_ID, pageable)).willReturn(page);

            // when
            Page<NotificationGetResponse> result = notificationService.getNotifications(USER_ID, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("존재하지 않는 유저 조회 시 예외가 발생한다")
        void getNotifications_userNotFound_throwsException() {
            // given
            given(userRepository.findById(any())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> notificationService.getNotifications(USER_ID, PageRequest.of(0, 20)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(UserErrorCode.ERR_USER_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("읽음 처리")
    class MarkAsRead {

        @Test
        @DisplayName("단건 읽음 처리 성공 시 readAt이 설정된다")
        void markAsRead_success() {
            // given
            Notification notification = makeNotification(USER_ID);
            given(notificationRepository.findById(NOTIFICATION_ID)).willReturn(Optional.of(notification));

            // when
            notificationService.markAsRead(USER_ID, NOTIFICATION_ID);

            // then
            assertThat(notification.isRead()).isTrue();
            assertThat(notification.getReadAt()).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 알림 읽음 처리 시 예외가 발생한다")
        void markAsRead_notFound_throwsException() {
            // given
            given(notificationRepository.findById(any())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> notificationService.markAsRead(USER_ID, NOTIFICATION_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(NotificationErrorCode.ERR_NOTIFICATION_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("다른 유저의 알림 읽음 처리 시 예외가 발생한다")
        void markAsRead_unauthorized_throwsException() {
            // given
            Notification notification = makeNotification(USER_ID);
            given(notificationRepository.findById(NOTIFICATION_ID)).willReturn(Optional.of(notification));

            // when & then
            assertThatThrownBy(() -> notificationService.markAsRead(OTHER_USER_ID, NOTIFICATION_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(NotificationErrorCode.ERR_NOTIFICATION_UNAUTHORIZED.getMessage());
        }

        @Test
        @DisplayName("전체 읽음 처리 시 markAllAsRead가 호출된다")
        void markAllAsRead_success() {
            // when
            notificationService.markAllAsRead(USER_ID);

            // then
            verify(notificationRepository).markAllAsRead(USER_ID);
        }
    }

    @Nested
    @DisplayName("알림 삭제")
    class DeleteNotification {

        @Test
        @DisplayName("단건 삭제 성공 시 delete가 호출된다")
        void deleteNotification_success() {
            // given
            Notification notification = makeNotification(USER_ID);
            given(notificationRepository.findById(NOTIFICATION_ID)).willReturn(Optional.of(notification));

            // when
            notificationService.deleteNotification(USER_ID, NOTIFICATION_ID);

            // then
            verify(notificationRepository).delete(notification);
        }

        @Test
        @DisplayName("존재하지 않는 알림 삭제 시 예외가 발생한다")
        void deleteNotification_notFound_throwsException() {
            // given
            given(notificationRepository.findById(any())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> notificationService.deleteNotification(USER_ID, NOTIFICATION_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(NotificationErrorCode.ERR_NOTIFICATION_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("다른 유저의 알림 삭제 시 예외가 발생한다")
        void deleteNotification_unauthorized_throwsException() {
            // given
            Notification notification = makeNotification(USER_ID);
            given(notificationRepository.findById(NOTIFICATION_ID)).willReturn(Optional.of(notification));

            // when & then
            assertThatThrownBy(() -> notificationService.deleteNotification(OTHER_USER_ID, NOTIFICATION_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(NotificationErrorCode.ERR_NOTIFICATION_UNAUTHORIZED.getMessage());
        }

        @Test
        @DisplayName("전체 삭제 시 deleteAllByUserId가 호출된다")
        void deleteAllNotifications_success() {
            // when
            notificationService.deleteAllNotifications(USER_ID);

            // then
            verify(notificationRepository).deleteAllByUserId(USER_ID);
        }
    }
}