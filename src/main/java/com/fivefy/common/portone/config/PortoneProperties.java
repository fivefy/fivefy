package com.fivefy.common.portone.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * API 키, 시크릿 설정
 * String apiSecret,    | 포트원 API 호출 인증키 — Authorization 헤더에 사용 (단건조회, 취소 등)   : 서버에서만 사용한다.
 * String storeId,      | 포트원이 Fivefy 상점에 부여한 식별자 — API 호출 시 어느 상점인지 명시     : API 호출 시 상점 식별
 * String channelKey,   | 결제 채널 식별자 — FE에서 포트원 SDK 결제창 호출 시 사용                 : FE에서 사용
 * String webhookSecret | 웹훅 시그니처 검증용 시크릿 — 위변조 방지 (HMAC-SHA256)                  : 서버에서만 사용
 */
@ConfigurationProperties(prefix = "portone")
public record PortoneProperties(
        String apiSecret,
        String storeId,
        String channelKey,
        String webhookSecret
) {}