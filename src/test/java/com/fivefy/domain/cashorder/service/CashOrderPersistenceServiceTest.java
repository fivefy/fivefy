package com.fivefy.domain.cashorder.service;

import com.fivefy.domain.billingkey.entity.BillingKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
class CashOrderPersistenceServiceTest {
    @Test
    void 정기포인트충전_성공시_구독갱신없이_빌링키_다음충전일만_갱신한다() {
        BillingKey billingKey = BillingKey.create(1L, "billing-key", "1234", "CARD", "현대카드");

        billingKey.scheduleNextCharge(LocalDateTime.of(2026, 5, 20, 17, 30));

        // 정기 충전 전 예정일
        assertThat(billingKey.getNextChargeDate())
                .isEqualTo(LocalDateTime.of(2026, 6, 20, 8, 0));

        billingKey.extendNextChargeOneMonth();

        // 정기 충전 후 예정일(1달 추가)
        assertThat(billingKey.getNextChargeDate())
                .isEqualTo(LocalDateTime.of(2026, 7, 20, 8, 0));
    }

}