package com.fivefy.domain.billingkey.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class BillingKeyTest {
    @Test
    void 정기구독_최초구매시_다음자동충전일은_한달뒤_8시다() {
        BillingKey billingKey = BillingKey.create(1L, "billing-key", "1234", "CARD", "현대카드");

        LocalDateTime now = LocalDateTime.of(2026, 5, 20, 17, 30);
        billingKey.scheduleNextCharge(now);

        assertThat(billingKey.getNextChargeDate())
                .isEqualTo(LocalDateTime.of(2026, 6, 20, 8, 0));
    }

    @Test
    void 자동충전_성공후_다음자동충전일은_한달뒤_8시다() {
        BillingKey billingKey = BillingKey.create(1L, "billing-key", "1234", "CARD", "현대카드");
        billingKey.scheduleNextCharge(LocalDateTime.of(2026, 5, 20, 17, 30));

        billingKey.extendNextChargeOneMonth();

        assertThat(billingKey.getNextChargeDate())
                .isEqualTo(LocalDateTime.of(2026, 7, 20, 8, 0));
    }

}