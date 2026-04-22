package com.fivefy.common.config.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationRabbitConfig {

    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String PUBLISH_TRACK_QUEUE = "notification.publish.track";
    public static final String PUBLISH_TRACK_ROUTING_KEY = "publish.track";

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Queue publishTrackQueue() {
        return new Queue(PUBLISH_TRACK_QUEUE, true); // durable = true
    }

    @Bean
    public Binding publishTrackBinding(Queue publishTrackQueue,
                                       DirectExchange notificationExchange) {
        return BindingBuilder
                .bind(publishTrackQueue)
                .to(notificationExchange)
                .with(PUBLISH_TRACK_ROUTING_KEY);
    }
}
