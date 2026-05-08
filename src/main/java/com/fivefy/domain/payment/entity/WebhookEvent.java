package com.fivefy.domain.payment.entity;

import com.fivefy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static com.fivefy.common.util.ValidationUtils.validateNonNull;

@Entity
@Getter
@Table(
    name = "webhook_events",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uc_webhook_events_event_payment",
            columnNames = {"webhook_event_id", "payment_id"}
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebhookEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 포트원이 부여하는 웹훅 고유 ID
     * 동일 웹훅 재전송 시 중복 판단 기준
     */
    @Column(name = "webhook_event_id", nullable = false)
    private String webhookEventId;

    /**
     * 결제 건 식별자 (포트원 pg_transaction_id와 대응)
     */
    @Column(name = "payment_id", nullable = false)
    private String paymentId;

    /**
     * 웹훅 수신 시각
     */
    @Column(nullable = false)
    private LocalDateTime receivedAt;

    /**
     * 웹훅 이벤트 저장
     *
     * @param webhookEventId 포트원 웹훅 고유 ID
     * @param paymentId      결제 건 식별자
     * @return WebhookEvent
     */
    public static WebhookEvent create(String webhookEventId, String paymentId) {
        validateNonNull(webhookEventId, "webhookEventId");
        validateNonNull(paymentId, "paymentId");

        WebhookEvent event     = new WebhookEvent();
            event.webhookEventId   = webhookEventId;
            event.paymentId        = paymentId;
            event.receivedAt       = LocalDateTime.now();

        return event;
    }
}