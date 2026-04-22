// ── PortoneBillingKeyResponse.java ───────────────────────────────────────────
package com.fivefy.common.portone.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 포트원 빌링키 단건 조회 응답
 * GET /billing-keys/{billingKeyId}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PortoneBillingKeyResponse(
        String billingKey,      // 빌링키 토큰
        CardInfo card,        // 카드 결제용 (카카오페이면 null)
        EasyPayInfo easyPay   // 간편결제용 (카드면 null)
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CardInfo(
            String name,
            String number
    ) {}

    // 문제가 있는데, 나는 테스트를 카카오페이로 함
    /**
     * billingKeyMethod: 'EASY_PAY' : CARD를 이걸로 바꾸면 된다 하는데
     * const response = await PortOne.requestIssueBillingKey({
     *     storeId,
     *     channelKey: channelKeyBilling, // 기존 KG이니시스 채널키
     *     billingKeyMethod: 'EASY_PAY',
     *     easyPay: {
     *         easyPayProvider: 'KAKAOPAY'
     *     },
     * });
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EasyPayInfo(
            String provider  // "KAKAOPAY", "NAVERPAY" 등
    ) {}
}