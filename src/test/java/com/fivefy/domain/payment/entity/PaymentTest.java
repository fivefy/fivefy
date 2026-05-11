package com.fivefy.domain.payment.entity;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.cashorder.entity.CashOrder;
import com.fivefy.domain.cashorder.enums.CashProductType;
import com.fivefy.domain.payment.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

class PaymentTest {

    // 모든 테스트에서 공통으로 사용할 CashOrder
    private CashOrder cashOrder;

    @BeforeEach
    void setUp() {
        // CashOrder.create()로 생성하면 @Id(id)가 null
        // → ReflectionTestUtils로 id를 강제 주입
        cashOrder = CashOrder.create(1L, CashProductType.PRODUCT_1, "ORD-12345678");
        ReflectionTestUtils.setField(cashOrder, "id", 1L);
    }

    // ─────────────────────────────────────────
    // create() — 초기 상태 검증
    // ─────────────────────────────────────────

    @Test
    @DisplayName("Payment 생성 시 초기 상태는 REQUESTED이다")
    void create_초기상태_REQUESTED() {
        CashOrder cashOrder = CashOrder.create(1L, CashProductType.PRODUCT_1, "ORD-12345678"); // 실제 create 시그니처에 맞게
        Payment payment = Payment.create(cashOrder, "pg-tx-001", "webhook-001");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REQUESTED);
        assertThat(payment.getPaidAt()).isNull();
        assertThat(payment.getRefundedAt()).isNull();
    }

    // ─────────────────────────────────────────
    // complete() — 결제 완료(REQUESTED)    → COMPLETED
    //              승인(COMPLETED)        → COMPLETED(멱등)    : 재호출
    // ─────────────────────────────────────────

    @Test
    @DisplayName("complete() 호출 시 COMPLETED로 전이되고 paidAt이 기록된다")
    void complete_REQUESTED에서_COMPLETED로() {
        // CashOrder Mock 객체 생성 후 전달
        CashOrder cashOrder = CashOrder.create(1L, CashProductType.PRODUCT_1, "ORD-12345678"); // 실제 create 시그니처에 맞게
        Payment payment = Payment.create(cashOrder, "pg-tx-001", "webhook-001");

        payment.complete();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getPaidAt()).isNotNull();
    }

    // ─────────────────────────────────────────
    // refund() — 결제 완료(REQUESTED)  → 환불(REFUNDED)
    //            승인(COMPLETED)      → 환불(REFUNDED)
    // ─────────────────────────────────────────

    @Test
    @DisplayName("COMPLETED 상태에서 refund() 호출 시 REFUNDED로 전이되고 refundedAt이 기록된다")
    void refund_COMPLETED에서_REFUNDED로() {
        CashOrder cashOrder = CashOrder.create(1L, CashProductType.PRODUCT_1, "ORD-12345678"); // 실제 create 시그니처에 맞게
        Payment payment = Payment.create(cashOrder, "pg-tx-001", "webhook-001");
        payment.complete();

        payment.refund("단순 변심");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getRefundedAt()).isNotNull();
        assertThat(payment.getRefundReason()).isEqualTo("단순 변심");
    }

    @Test
    @DisplayName("REFUNDED 상태에서 refund() 재호출 시 예외가 발생한다")
    void refund_REFUNDED에서_재호출_예외() {
        CashOrder cashOrder = CashOrder.create(1L, CashProductType.PRODUCT_1, "ORD-12345678"); // 실제 create 시그니처에 맞게
        Payment payment = Payment.create(cashOrder, "pg-tx-001", "webhook-001");
        payment.complete();
        payment.refund("첫 번째 환불");

        assertThatThrownBy(() -> payment.refund("두 번째 환불"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("refund() 호출 시 reason이 null이면 예외가 발생한다")
    void refund_reason이_null이면_예외() {
        CashOrder cashOrder = CashOrder.create(1L, CashProductType.PRODUCT_1, "ORD-12345678"); // 실제 create 시그니처에 맞게
        Payment payment = Payment.create(cashOrder, "pg-tx-001", "webhook-001");
        payment.complete();

        assertThatThrownBy(() -> payment.refund(null))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("COMPLETED 상태에서 complete() 재호출 시 예외 없이 멱등 처리된다")
    void complete_COMPLETED에서_재호출_멱등() {
        CashOrder cashOrder = CashOrder.create(1L, CashProductType.PRODUCT_1, "ORD-12345678"); // 실제 create 시그니처에 맞게
        Payment payment = Payment.create(cashOrder, "pg-tx-001", "webhook-001");
        payment.complete();

        // assertThatCode는 예외가 없어야 통과하는 케이스
        assertThatCode(() -> payment.complete())
                .doesNotThrowAnyException();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }
}