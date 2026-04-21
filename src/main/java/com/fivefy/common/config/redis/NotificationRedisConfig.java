package com.fivefy.common.config.redis;

import com.fivefy.domain.notification.service.NotificationRedisSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
@RequiredArgsConstructor
public class NotificationRedisConfig {

    private static final String NOTIFICATION_CHANNEL_PATTERN = "notification:*";

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter notificationListenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
                notificationListenerAdapter,
                new PatternTopic(NOTIFICATION_CHANNEL_PATTERN)
        );
        return container;
    }

    @Bean
    public MessageListenerAdapter notificationListenerAdapter(
            NotificationRedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }
}