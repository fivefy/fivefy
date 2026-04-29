package com.fivefy.domain.payment.entity;

import com.fivefy.domain.payment.enums.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    // ─────────────────────────────────────────
    // create() — 초기 상태 검증
    // ─────────────────────────────────────────

    @Test
    @DisplayName("Payment 생성 시 초기 상태는 REQUESTED이다")
    void create_초기상태_REQUESTED() {
        Payment payment = Payment.create(1L, 1000L, "ORD-12345678", "pg-tx-001", "webhook-001");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REQUESTED);
        assertThat(payment.getPaidAt()).isNull();
        assertThat(payment.getRefundedAt()).isNull();
    }

    // ─────────────────────────────────────────
    // complete() — REQUESTED → COMPLETED
    // ─────────────────────────────────────────

    @Test
    @DisplayName("complete() 호출 시 COMPLETED로 전이되고 paidAt이 기록된다")
    void complete_REQUESTED에서_COMPLETED로() {
        Payment payment = Payment.create(1L, 1000L, "ORD-12345678", "pg-tx-001", "webhook-001");

        payment.complete();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getPaidAt()).isNotNull();
    }

    // ─────────────────────────────────────────
    // refund() — COMPLETED → REFUNDED
    // ─────────────────────────────────────────

    @Test
    @DisplayName("COMPLETED 상태에서 refund() 호출 시 REFUNDED로 전이되고 refundedAt이 기록된다")
    void refund_COMPLETED에서_REFUNDED로() {
        Payment payment = Payment.create(1L, 1000L, "ORD-12345678", "pg-tx-001", "webhook-001");
        payment.complete();

        payment.refund("단순 변심");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getRefundedAt()).isNotNull();
        assertThat(payment.getRefundReason()).isEqualTo("단순 변심");
    }

    @Test
    @DisplayName("refund() 호출 시 reason이 null이면 예외가 발생한다")
    void refund_reason이_null이면_예외() {
        Payment payment = Payment.create(1L, 1000L, "ORD-12345678", "pg-tx-001", "webhook-001");
        payment.complete();

        assertThatThrownBy(() -> payment.refund(null))
                .isInstanceOf(Exception.class);
    }
}