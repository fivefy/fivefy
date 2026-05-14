package com.fivefy.domain.subscription.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public enum SubscriptionPlanType {
    FREE(0L,  "3일 체험"),                       // 무료(1회)     : 다음 갱신일 없음
    RECURRING(50L, "구독"),                 // 포인트 단건    : 다음 갱신일 없음
    RECURRING_AUTO(50L, "정기 구독 (카드 자동)"); // 카드 자동      : 다음 갱신일 있음

    private final Long price;
    private final String description;

    /**
     * 활성화 시점 기준으로 만료일 계산
     * RECURRING은 activate() 시점에 + 1개월
     * @param startDate
     * @return
     */
    public LocalDateTime calculateExpiryDate(LocalDateTime startDate) {
        return switch (this) {
            case FREE -> startDate.plusDays(3);
            case RECURRING, RECURRING_AUTO -> startDate.plusMonths(1);
        };
    }

    // 카드 자동 청구 방식인지 여부 : 다음 갱신일
    public boolean isAutoRenew() {
        return this == RECURRING_AUTO;
    }
}