package com.fivefy.domain.user.listener;

import com.fivefy.domain.user.dto.event.UserDeletedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserEventListenerTest {

    @InjectMocks
    private UserEventListener userEventListener;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("탈퇴 이벤트 수신 시 RT + PREV_RT 삭제")
    void handleUserDeleted() {
        // given
        UserDeletedEvent event = new UserDeletedEvent(1L);

        // when
        userEventListener.handleUserDeleted(event);

        // then
        verify(redisTemplate).delete("RT:1");
        verify(redisTemplate).delete("PREV_RT:1");
    }

}