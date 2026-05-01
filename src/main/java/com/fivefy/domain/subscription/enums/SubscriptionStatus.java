package com.fivefy.domain.subscription.enums;

public enum SubscriptionStatus {
    FREE,       // 체험 (구독 플랜 타입 : FREE)
    ACTIVE,     // 활성
    INACTIVE,   // 비활성화 (결제 완료 했으나, 활성화 안 함)
    EXPIRE,     // 만료
    CANCELED,   // 취소
    REFUND      // 환불(거의 안씀)
}
