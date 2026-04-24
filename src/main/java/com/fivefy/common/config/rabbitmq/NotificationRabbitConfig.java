package com.fivefy.common.config.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class NotificationRabbitConfig {

    // Exchange
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String DEAD_LETTER_EXCHANGE = "notification.dlx";

    // Queue
    public static final String PUBLISH_TRACK_QUEUE = "notification.publish.track";
    public static final String PUBLISH_TRACK_DLQ = "notification.publish.track.dlq";

    // Routing Key
    public static final String PUBLISH_TRACK_ROUTING_KEY = "publish.track";
    public static final String PUBLISH_TRACK_DLQ_ROUTING_KEY = "publish.track.dead";

    // 최대 재시도 횟수
    public static final int MAX_RETRY_COUNT = 3;
    public static final String RETRY_QUEUE = "notification.publish.track.retry";
    public static final String RETRY_ROUTING_KEY = "publish.track.retry";

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE);
    }

    // 메인 Queue — DLX 설정으로 실패 시 DLQ로 이동
    @Bean
    public Queue publishTrackQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", NOTIFICATION_EXCHANGE);
        args.put("x-dead-letter-routing-key", RETRY_ROUTING_KEY);
        return new Queue(PUBLISH_TRACK_QUEUE, true, false, false, args);
    }

    // Dead Letter Queue — 최종 실패 메시지 보관
    @Bean
    public Queue publishTrackDlq() {
        return new Queue(PUBLISH_TRACK_DLQ, true); // durable = true
    }

    @Bean
    public Binding publishTrackBinding(Queue publishTrackQueue,DirectExchange notificationExchange) {
        return BindingBuilder
                .bind(publishTrackQueue)
                .to(notificationExchange)
                .with(PUBLISH_TRACK_ROUTING_KEY);
    }

    @Bean
    public Binding publishTrackDlqBinding(Queue publishTrackDlq,DirectExchange deadLetterExchange) {
        return BindingBuilder
                .bind(publishTrackDlq)
                .to(deadLetterExchange)
                .with(PUBLISH_TRACK_DLQ_ROUTING_KEY);
    }

    @Bean
    public Binding retryBinding(Queue publishTrackRetryQueue,
                                DirectExchange notificationExchange) {
        return BindingBuilder
                .bind(publishTrackRetryQueue)
                .to(notificationExchange)
                .with(RETRY_ROUTING_KEY);
    }

    // Manual ACK 설정
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(3);
        return factory;
    }

    @Bean
    public Queue publishTrackRetryQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", NOTIFICATION_EXCHANGE);
        args.put("x-dead-letter-routing-key", PUBLISH_TRACK_ROUTING_KEY);
        args.put("x-message-ttl", 5000); // 5초 후 재시도

        return new Queue(RETRY_QUEUE, true, false, false, args);
    }
}
