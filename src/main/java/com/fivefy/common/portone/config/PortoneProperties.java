package com.fivefy.common.portone.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * API 키, 시크릿 설정
 */
@ConfigurationProperties(prefix = "portone")
public record PortoneProperties(
        String apiSecret,
        String storeId,
        String channelKey,
        String webhookSecret
) {}