package com.fivefy.domain.subscription.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public enum SubscriptionPlanType {
    FREE(0L,      "3일 체험"),
    RECURRING(50L, "정기 구독");

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
            case FREE     -> startDate.plusDays(3);     // 무료 플랜 3일
            case RECURRING -> startDate.plusMonths(1);  // 정기 결제
        };
    }
}