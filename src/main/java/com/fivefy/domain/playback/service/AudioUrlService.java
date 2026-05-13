package com.fivefy.domain.playback.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

/**
 * CloudFront 기반 audio URL 생성 서비스
 */
@Service
public class AudioUrlService {

    private final String cloudFrontDomain;

    public AudioUrlService(
            @Value("${cloudfront.audio-domain}") String cloudFrontDomain
    ) {
        this.cloudFrontDomain = normalizeDomain(cloudFrontDomain);
    }

    public String createAudioUrl(String audioKey) {
        if (audioKey == null || audioKey.isBlank()) {
            return null;
        }

        String encodedKey =
                UriUtils.encodePath(audioKey, StandardCharsets.UTF_8);

        return cloudFrontDomain + "/" + encodedKey;
    }

    private String normalizeDomain(String domain) {
        if (domain.endsWith("/")) {
            return domain.substring(0, domain.length() - 1);
        }

        return domain;
    }
}
