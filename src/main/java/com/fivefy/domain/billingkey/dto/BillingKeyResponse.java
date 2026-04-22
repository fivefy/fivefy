package com.fivefy.domain.billingkey.dto;

import com.fivefy.domain.billingkey.entity.BillingKey;

/**
 * 빌링키 등록/조회 응답
 * billingKey 토큰은 보안상 응답에서 제외
 */
public record BillingKeyResponse(
        Long id,
        String cardName,
        String cardLast4,   // 카카오페이면 null
        String payMethod,   // "CARD" or "KAKAOPAY"
        boolean active
) {
    public static BillingKeyResponse from(BillingKey billingKey) {
        return new BillingKeyResponse(
                billingKey.getId(),
                billingKey.getCardName(),
                billingKey.getCardLast4(),
                billingKey.getPayMethod(),
                billingKey.isActive()
        );
    }
}
