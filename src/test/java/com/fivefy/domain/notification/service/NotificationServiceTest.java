package com.fivefy.domain.notification.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.follow.repository.FollowRepository;
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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.ObjectMapper;

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

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private SseEmitterRepository sseEmitterRepository;
    @Mock private FollowRepository followRepository;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ObjectMapper objectMapper;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private NotificationService notificationService;

    private static final Long USER_ID          = 1L;
    private static final Long OTHER_USER_ID    = 2L;
    private static final Long NOTIFICATION_ID  = 10L;
    private static final Long LAST_EVENT_ID    = 5L;

    private Notification makeNotification(Long userId) {
        return Notification.create(userId, "테스트 알림", NotificationType.NEW_FOLLOWER,
                NotificationChannel.IN_APP, null, null, null);
    }

    @Nested
    @DisplayName("SSE 구독")
    class Subscribe {

        @Test
        @DisplayName("최초 구독 시 SseEmitter를 저장하고 초기 이벤트를 전송한다")
        void subscribe_savesEmitterAndSendsConnectEvent() {
            // given
            given(notificationRepository.countByUserIdAndReadAtIsNull(USER_ID)).willReturn(3L);

            // when
            SseEmitter emitter = notificationService.subscribe(USER_ID, null);

            // then
            assertThat(emitter).isNotNull();
            verify(sseEmitterRepository).save(eq(USER_ID), any(SseEmitter.class));
            verify(notificationRepository).countByUserIdAndReadAtIsNull(USER_ID);
        }

        @Test
        @DisplayName("최초 구독 시 lastEventId가 null이면 미수신 알림 조회를 하지 않는다")
        void subscribe_nullLastEventId_skipsReplay() {
            // given
            given(notificationRepository.countByUserIdAndReadAtIsNull(USER_ID)).willReturn(0L);

            // when
            notificationService.subscribe(USER_ID, null);

            // then
            verify(notificationRepository, never()).findMissedNotifications(any(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("재연결 시 lastEventId 이후의 미수신 알림을 SSE로 재전송한다")
        void subscribe_withLastEventId_replaysMissedNotifications() {
            // given
            Notification missed1 = makeNotification(USER_ID);
            Notification missed2 = makeNotification(USER_ID);

            given(notificationRepository.countByUserIdAndReadAtIsNull(USER_ID)).willReturn(2L);
            given(notificationRepository.findMissedNotifications(eq(USER_ID), eq(LAST_EVENT_ID), any(Pageable.class)))
                    .willReturn(List.of(missed1, missed2));

            // when
            SseEmitter emitter = notificationService.subscribe(USER_ID, LAST_EVENT_ID);

            // then
            assertThat(emitter).isNotNull();
            verify(notificationRepository).findMissedNotifications(eq(USER_ID), eq(LAST_EVENT_ID), any(Pageable.class));
        }

        @Test
        @DisplayName("재연결 시 미수신 알림이 없으면 재전송하지 않는다")
        void subscribe_withLastEventId_noMissed_noReplay() {
            // given
            given(notificationRepository.countByUserIdAndReadAtIsNull(USER_ID)).willReturn(0L);
            given(notificationRepository.findMissedNotifications(eq(USER_ID), eq(LAST_EVENT_ID), any(Pageable.class)))
                    .willReturn(List.of());

            // when
            notificationService.subscribe(USER_ID, LAST_EVENT_ID);

            // then — findMissedNotifications는 호출되지만 emitter.send()는 0번
            verify(notificationRepository).findMissedNotifications(eq(USER_ID), eq(LAST_EVENT_ID), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("알림 발송")
    class Send {

        @Test
        @DisplayName("Redis publish 성공 시 SENT 상태로 저장된다")
        void send_redisSuccess_marksAsSent() throws Exception {
            // given
            Notification notification = makeNotification(USER_ID);
            given(notificationRepository.save(any())).willReturn(notification);
            given(objectMapper.writeValueAsString(any())).willReturn("{}");

            // when
            notificationService.send(USER_ID, NotificationType.NEW_FOLLOWER, "테스트 알림", null, null);

            // then
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
            verify(stringRedisTemplate).convertAndSend(any(), any(String.class));
            verify(notificationRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("Redis publish 성공 시 fallbackSseDirectPush를 호출하지 않는다")
        void send_redisSuccess_doesNotCallFallback() throws Exception {
            // given
            Notification notification = makeNotification(USER_ID);
            given(notificationRepository.save(any())).willReturn(notification);
            given(objectMapper.writeValueAsString(any())).willReturn("{}");

            // when
            notificationService.send(USER_ID, NotificationType.NEW_FOLLOWER, "테스트 알림", null, null);

            // then
            verify(sseEmitterRepository, never()).findAllByUserId(any());
        }

        @Test
        @DisplayName("Redis publish 실패 시 FAILED 상태로 저장된다")
        void send_redisFails_marksAsFailed() throws Exception {
            // given
            Notification notification = makeNotification(USER_ID);
            given(notificationRepository.save(any())).willReturn(notification);
            given(objectMapper.writeValueAsString(any())).willReturn("{}");
            given(stringRedisTemplate.convertAndSend(any(), any(String.class)))
                    .willThrow(new RuntimeException("Redis 장애"));
            given(sseEmitterRepository.findAllByUserId(USER_ID)).willReturn(List.of());

            // when
            notificationService.send(USER_ID, NotificationType.NEW_FOLLOWER, "테스트 알림", null, null);

            // then
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
            verify(notificationRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("Redis publish 실패 시 연결된 SSE emitter에 직접 전송한다")
        void send_redisFails_fallbackSseDirectPush() throws Exception {
            // given
            Notification notification = makeNotification(USER_ID);
            SseEmitter emitter = mock(SseEmitter.class);

            given(notificationRepository.save(any())).willReturn(notification);
            given(objectMapper.writeValueAsString(any())).willReturn("{}");
            given(stringRedisTemplate.convertAndSend(any(), any(String.class)))
                    .willThrow(new RuntimeException("Redis 장애"));
            given(sseEmitterRepository.findAllByUserId(USER_ID)).willReturn(List.of(emitter));

            // when
            notificationService.send(USER_ID, NotificationType.NEW_FOLLOWER, "테스트 알림", null, null);

            // then
            verify(sseEmitterRepository).findAllByUserId(USER_ID);
            verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        }

        @Test
        @DisplayName("Redis publish 실패 시 연결된 emitter가 없으면 SSE 전송을 시도하지 않는다")
        void send_redisFails_noEmitter_skipsDirectPush() throws Exception {
            // given
            Notification notification = makeNotification(USER_ID);
            given(notificationRepository.save(any())).willReturn(notification);
            given(objectMapper.writeValueAsString(any())).willReturn("{}");
            given(stringRedisTemplate.convertAndSend(any(), any(String.class)))
                    .willThrow(new RuntimeException("Redis 장애"));
            given(sseEmitterRepository.findAllByUserId(USER_ID)).willReturn(List.of());

            // when
            notificationService.send(USER_ID, NotificationType.NEW_FOLLOWER, "테스트 알림", null, null);

            // then — emitter.send() 호출 없음
            verify(sseEmitterRepository).findAllByUserId(USER_ID);
        }

        @Test
        @DisplayName("SSE 직접 전송 중 IOException 발생 시 해당 emitter를 삭제한다")
        void send_fallbackSseDirectPush_ioException_removesEmitter() throws Exception {
            // given
            Notification notification = makeNotification(USER_ID);
            SseEmitter brokenEmitter = mock(SseEmitter.class);

            given(notificationRepository.save(any())).willReturn(notification);
            given(objectMapper.writeValueAsString(any())).willReturn("{}");
            given(stringRedisTemplate.convertAndSend(any(), any(String.class)))
                    .willThrow(new RuntimeException("Redis 장애"));
            given(sseEmitterRepository.findAllByUserId(USER_ID)).willReturn(List.of(brokenEmitter));
            doThrow(new IOException("연결 끊김")).when(brokenEmitter).send(any(SseEmitter.SseEventBuilder.class));

            // when
            notificationService.send(USER_ID, NotificationType.NEW_FOLLOWER, "테스트 알림", null, null);

            // then
            verify(sseEmitterRepository).delete(eq(USER_ID), eq(brokenEmitter));
        }

        @Test
        @DisplayName("알림 저장 후 Redis publish를 항상 시도한다")
        void send_alwaysPublishesToRedis() throws Exception {
            // given
            Notification notification = makeNotification(USER_ID);
            given(notificationRepository.save(any())).willReturn(notification);
            given(objectMapper.writeValueAsString(any())).willReturn("{}");

            // when
            notificationService.send(USER_ID, NotificationType.NEW_FOLLOWER, "테스트 알림", null, null);

            // then
            verify(stringRedisTemplate).convertAndSend(any(), any(String.class));
        }

        @Test
        @DisplayName("동일 idempotency key로 중복 발송 시 스킵된다")
        void send_duplicateIdempotencyKey_skips() throws Exception {
            // given
            Notification notification = makeNotification(USER_ID);
            given(objectMapper.writeValueAsString(any())).willReturn("{}");

            given(notificationRepository.save(any()))
                    .willReturn(notification)   // 1번째 send() 최초 저장
                    .willReturn(notification)   // 1번째 send() markAsSent 저장
                    .willThrow(new DataIntegrityViolationException("중복 키")); // 2번째 send() 중복 차단

            // when
            notificationService.send(USER_ID, NotificationType.SUBSCRIBE, "구독 시작", null, null);
            notificationService.send(USER_ID, NotificationType.SUBSCRIBE, "구독 시작", null, null);

            // then — Redis publish는 1번만 호출
            verify(stringRedisTemplate, times(1)).convertAndSend(any(), any(String.class));
        }

        @Test
        @DisplayName("다른 actorId의 NEW_FOLLOWER 알림은 중복으로 처리되지 않는다")
        void send_differentActorId_notDuplicate() throws Exception {
            // given
            Notification notification = makeNotification(USER_ID);
            given(notificationRepository.save(any())).willReturn(notification);
            given(objectMapper.writeValueAsString(any())).willReturn("{}");

            // when — 다른 사람이 팔로우 (actorId 다름)
            notificationService.send(USER_ID, NotificationType.NEW_FOLLOWER, "A 팔로우", 1L, null);
            notificationService.send(USER_ID, NotificationType.NEW_FOLLOWER, "B 팔로우", 2L, null);

            // then — 둘 다 Redis publish 호출
            verify(stringRedisTemplate, times(2)).convertAndSend(any(), any(String.class));
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