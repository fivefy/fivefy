package com.fivefy.domain.subscription.entity;

import com.fivefy.domain.subscription.enums.SubscriptionPlanType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
class SubscriptionBillingDateTest {
    @Test
    void 정기자동구독_생성시_다음구독결제일은_한달뒤_9시다() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 20, 17, 30);

        Subscription subscription = Subscription.create(
                1L,
                SubscriptionPlanType.RECURRING_AUTO,
                now
        );

        assertThat(subscription.getNextBillingDate())
                .isEqualTo(LocalDateTime.of(2026, 6, 20, 9, 0));
    }

}