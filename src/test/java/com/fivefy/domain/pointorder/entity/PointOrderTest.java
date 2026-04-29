package com.fivefy.domain.pointorder.entity;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.pointorder.enums.PointOrderStatus;
import com.fivefy.domain.subscription.enums.SubscriptionPlanType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PointOrderTest {

    // ─────────────────────────────────────────
    // create() — 초기 상태 검증
    // ─────────────────────────────────────────

    @Test
    @DisplayName("PointOrder 생성 시 초기 상태는 PENDING이다")
    void create_초기상태_PENDING() {
        PointOrder pointOrder = PointOrder.create(1L, SubscriptionPlanType.RECURRING, "SUB-12345678");

        assertThat(pointOrder.getStatus()).isEqualTo(PointOrderStatus.PENDING);
    }

    // ─────────────────────────────────────────
    // success() — PENDING → SUCCESS
    // ─────────────────────────────────────────

    @Test
    @DisplayName("PENDING 상태에서 success() 호출 시 SUCCESS로 전이된다")
    void success_PENDING에서_SUCCESS로() {
        PointOrder pointOrder = PointOrder.create(1L, SubscriptionPlanType.RECURRING, "SUB-12345678");

        pointOrder.success();

        assertThat(pointOrder.getStatus()).isEqualTo(PointOrderStatus.SUCCESS);
    }

    @Test
    @DisplayName("SUCCESS 상태에서 success() 재호출 시 예외가 발생한다")
    void success_SUCCESS에서_재호출_예외() {
        PointOrder pointOrder = PointOrder.create(1L, SubscriptionPlanType.RECURRING, "SUB-12345678");
        pointOrder.success();

        assertThatThrownBy(() -> pointOrder.success())
                .isInstanceOf(BusinessException.class);
    }

    // ─────────────────────────────────────────
    // refund() — SUCCESS → REFUNDED
    // ─────────────────────────────────────────

    @Test
    @DisplayName("SUCCESS 상태에서 refund() 호출 시 REFUNDED로 전이된다")
    void refund_SUCCESS에서_REFUNDED로() {
        PointOrder pointOrder = PointOrder.create(1L, SubscriptionPlanType.RECURRING, "SUB-12345678");
        pointOrder.success();

        pointOrder.refund();

        assertThat(pointOrder.getStatus()).isEqualTo(PointOrderStatus.REFUNDED);
    }

    @Test
    @DisplayName("PENDING 상태에서 refund() 호출 시 예외가 발생한다")
    void refund_PENDING에서_호출_예외() {
        PointOrder pointOrder = PointOrder.create(1L, SubscriptionPlanType.RECURRING, "SUB-12345678");

        assertThatThrownBy(() -> pointOrder.refund())
                .isInstanceOf(BusinessException.class);
    }
}