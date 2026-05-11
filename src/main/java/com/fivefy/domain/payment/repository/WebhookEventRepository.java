package com.fivefy.domain.payment.repository;

import com.fivefy.domain.payment.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    // 순차 중복 체크용
    boolean existsByWebhookEventIdAndPaymentId(String webhookEventId, String paymentId);
}