package com.fivefy.domain.user.listener;

import com.fivefy.domain.user.dto.event.UserDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final StringRedisTemplate redisTemplate;
    private static final String RT_PREFIX = "RT:";
    private static final String PREV_RT_PREFIX = "PREV_RT:";

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserDeleted(UserDeletedEvent event) {
        redisTemplate.delete(RT_PREFIX + event.userId());
        redisTemplate.delete(PREV_RT_PREFIX + event.userId());
    }
}
