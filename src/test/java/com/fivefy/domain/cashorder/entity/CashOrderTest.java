package com.fivefy.domain.cashorder.entity;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.cashorder.enums.CashOrderStatus;
import com.fivefy.domain.cashorder.enums.CashProductType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CashOrderTest {

    // ─────────────────────────────────────────
    // create() — 초기 상태 검증
    // ─────────────────────────────────────────

    @Test
    @DisplayName("CashOrder 생성 시 초기 상태는 PENDING이다")
    void create_초기상태_PENDING() {
        CashOrder cashOrder = CashOrder.create(1L, CashProductType.PRODUCT_1, "ORD-12345678");

        assertThat(cashOrder.getStatus()).isEqualTo(CashOrderStatus.PENDING);
    }

    // ─────────────────────────────────────────
    // success() — PENDING → SUCCESS
    // ─────────────────────────────────────────

    @Test
    @DisplayName("PENDING 상태에서 success() 호출 시 SUCCESS로 전이된다")
    void success_PENDING에서_SUCCESS로() {
        CashOrder cashOrder = CashOrder.create(1L, CashProductType.PRODUCT_1, "ORD-12345678");

        cashOrder.success("webhook-id-001");

        assertThat(cashOrder.getStatus()).isEqualTo(CashOrderStatus.SUCCESS);
        assertThat(cashOrder.getWebhookId()).isEqualTo("webhook-id-001");
    }

    @Test
    @DisplayName("SUCCESS 상태에서 success() 재호출 시 예외가 발생한다")
    void success_SUCCESS에서_재호출_예외() {
        CashOrder cashOrder = CashOrder.create(1L, CashProductType.PRODUCT_1, "ORD-12345678");
        cashOrder.success("webhook-id-001");

        assertThatThrownBy(() -> cashOrder.success("webhook-id-002"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("REFUNDED 상태에서 success() 호출 시 예외가 발생한다")
    void success_REFUNDED에서_호출_예외() {
        CashOrder cashOrder = CashOrder.create(1L, CashProductType.PRODUCT_1, "ORD-12345678");
        cashOrder.success("webhook-id-001");
        cashOrder.refund();

        assertThatThrownBy(() -> cashOrder.success("webhook-id-002"))
                .isInstanceOf(BusinessException.class);
    }

    // ─────────────────────────────────────────
    // refund() — SUCCESS → REFUNDED
    // ─────────────────────────────────────────

    @Test
    @DisplayName("SUCCESS 상태에서 refund() 호출 시 REFUNDED로 전이된다")
    void refund_SUCCESS에서_REFUNDED로() {
        CashOrder cashOrder = CashOrder.create(1L, CashProductType.PRODUCT_1, "ORD-12345678");
        cashOrder.success("webhook-id-001");

        cashOrder.refund();

        assertThat(cashOrder.getStatus()).isEqualTo(CashOrderStatus.REFUNDED);
    }

    @Test
    @DisplayName("PENDING 상태에서 refund() 호출 시 예외가 발생한다")
    void refund_PENDING에서_호출_예외() {
        CashOrder cashOrder = CashOrder.create(1L, CashProductType.PRODUCT_1, "ORD-12345678");

        assertThatThrownBy(() -> cashOrder.refund())
                .isInstanceOf(BusinessException.class);
    }
}