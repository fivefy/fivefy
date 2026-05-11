package com.fivefy.domain.subscription.entity;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.subscription.enums.SubscriptionPlanType;
import com.fivefy.domain.subscription.enums.SubscriptionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionTest {

    private static final LocalDateTime NOW = LocalDateTime.now();

    // ─────────────────────────────────────────
    // create() — 초기 상태 검증
    // ─────────────────────────────────────────

    @Test
    @DisplayName("구독 생성 시 초기 상태는 ACTIVE이다")
    void create_초기상태_ACTIVE() {
        // userID 삭제됐으니, 인수 하나 줄어듦
        Subscription subscription = Subscription.create(1L, SubscriptionPlanType.RECURRING, NOW);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("RECURRING 구독 생성 시 nextBillingDate가 1개월 후로 설정된다")
    void create_RECURRING_nextBillingDate_1개월후() {
        Subscription subscription = Subscription.create(1L, SubscriptionPlanType.RECURRING_AUTO, NOW);

        assertThat(subscription.getNextBillingDate()).isNotNull();
        assertThat(subscription.getNextBillingDate()).isAfter(NOW);
    }

    @Test
    @DisplayName("FREE 구독 생성 시 nextBillingDate는 null이다")
    void create_FREE_nextBillingDate_null() {
        Subscription subscription = Subscription.create(1L, SubscriptionPlanType.FREE, NOW);

        assertThat(subscription.getNextBillingDate()).isNull();
    }

    // ─────────────────────────────────────────
    // cancel() — ACTIVE → CANCELED
    // ─────────────────────────────────────────

    @Test
    @DisplayName("ACTIVE 상태에서 cancel() 호출 시 CANCELED로 전이되고 nextBillingDate가 null이 된다")
    void cancel_ACTIVE에서_CANCELED로() {
        Subscription subscription = Subscription.create(1L, SubscriptionPlanType.RECURRING_AUTO, NOW);

        subscription.cancel();

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        assertThat(subscription.getNextBillingDate()).isNull();
    }

    @Test
    @DisplayName("FREE 구독은 cancel() 호출 시 예외가 발생한다")
    void cancel_FREE_예외() {
        Subscription subscription = Subscription.create(1L, SubscriptionPlanType.FREE, NOW);

        assertThatThrownBy(() -> subscription.cancel())
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("CANCELED 상태에서 cancel() 재호출 시 예외가 발생한다")
    void cancel_CANCELED에서_재호출_예외() {
        Subscription subscription = Subscription.create(1L, SubscriptionPlanType.RECURRING, NOW);
        subscription.cancel();

        assertThatThrownBy(() -> subscription.cancel())
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("EXPIRE 상태에서 cancel() 호출 시 예외가 발생한다")
    void cancel_EXPIRE에서_호출_예외() {
        Subscription subscription = Subscription.create(1L, SubscriptionPlanType.RECURRING, NOW);
        subscription.expire();

        assertThatThrownBy(() -> subscription.cancel())
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("RECURRING 구독 생성 시 nextBillingDate는 null이다")
    void create_RECURRING_nextBillingDate_null() {
        Subscription subscription = Subscription.create(1L,  SubscriptionPlanType.RECURRING, NOW);

        assertThat(subscription.getNextBillingDate()).isNull();
    }

    // ─────────────────────────────────────────
    //  expire() —  활성화(ACTIVE)    → 만료(EXPIRE)
    //              취소(CANCLE)      → 만료(EXPIRE)
    // ─────────────────────────────────────────

    @Test
    @DisplayName("expire() 호출 시 EXPIRE로 전이되고 nextBillingDate가 null이 된다")
    void expire_EXPIRE로_전이() {
        Subscription subscription = Subscription.create(1L, SubscriptionPlanType.RECURRING, NOW);

        subscription.expire();

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.EXPIRE);
        assertThat(subscription.getNextBillingDate()).isNull();
    }

    @Test
    @DisplayName("CANCELED 상태에서 expire() 호출 시 EXPIRE로 전이된다 — 취소 후 만료일 지나면 만료 처리")
    void expire_CANCELED에서_EXPIRE로() {
        Subscription subscription = Subscription.create(1L, SubscriptionPlanType.RECURRING, NOW);
        subscription.cancel();

        subscription.expire();

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.EXPIRE);
    }

    @Test
    @DisplayName("EXPIRE 상태에서 expire() 재호출 시 예외가 발생한다 — 만료 재호출 차단")
    void expire_EXPIRE에서_재호출_예외() {
        Subscription subscription = Subscription.create(1L,  SubscriptionPlanType.RECURRING, NOW);
        subscription.expire();

        assertThatThrownBy(() -> subscription.expire())
                .isInstanceOf(BusinessException.class);
    }

    // ─────────────────────────────────────────
    // renew() — ACTIVE 유지, 날짜 갱신
    // ─────────────────────────────────────────

    @Test
    @DisplayName("RECURRING 구독에서 renew() 호출 시 expiryDate와 nextBillingDate가 1개월 연장된다")
    void renew_날짜_1개월_연장() {
        Subscription subscription = Subscription.create(1L,  SubscriptionPlanType.RECURRING_AUTO, NOW);
        LocalDateTime beforeExpiry = subscription.getExpiryDate();
        LocalDateTime beforeBilling = subscription.getNextBillingDate();

        subscription.renew();

        assertThat(subscription.getExpiryDate()).isEqualTo(beforeExpiry.plusMonths(1));
        assertThat(subscription.getNextBillingDate()).isEqualTo(beforeBilling.plusMonths(1));
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("FREE 구독에서 renew() 호출 시 예외가 발생한다")
    void renew_FREE_예외() {
        Subscription subscription = Subscription.create(1L,  SubscriptionPlanType.FREE, NOW);

        assertThatThrownBy(() -> subscription.renew())
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("취소된 구독(nextBillingDate=null)에서 renew() 호출 시 예외가 발생한다")
    void renew_취소된구독_예외() {
        Subscription subscription = Subscription.create(1L,  SubscriptionPlanType.RECURRING, NOW);
        subscription.cancel();

        assertThatThrownBy(() -> subscription.renew())
                .isInstanceOf(BusinessException.class);
    }

    // ─────────────────────────────────────────
    // isActive() — 활성 여부 확인
    // ─────────────────────────────────────────

    @Test
    @DisplayName("ACTIVE 상태이고 만료일이 지나지 않았으면 isActive()는 true다")
    void isActive_true() {
        Subscription subscription = Subscription.create(1L, SubscriptionPlanType.RECURRING, NOW);

        assertThat(subscription.isActive()).isTrue();
    }

    @Test
    @DisplayName("CANCELED 상태이면 isActive()는 false다")
    void isActive_CANCELED_false() {
        Subscription subscription = Subscription.create(1L, SubscriptionPlanType.RECURRING, NOW);
        subscription.cancel();

        assertThat(subscription.isActive()).isFalse();
    }
}