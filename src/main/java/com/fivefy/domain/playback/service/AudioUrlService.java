package com.fivefy.domain.playback.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

/**
 * 저장된 audioKey를 실제 재생 가능한 audioUrl로 변환하는 서비스
 */
@Service
public class AudioUrlService {

    // CloudFront 또는 S3 접근 도메인
    private final String cloudFrontDomain;

    public AudioUrlService(
            @Value("${cloudfront.audio-domain}") String cloudFrontDomain
    ) {
        // URL 중복 방지를 위해 마지막 "/" 제거
        this.cloudFrontDomain = normalizeDomain(cloudFrontDomain);
    }

    /**
     * audioKey를 기반으로 실제 접근 가능한 audioUrl 생성
     */
    public String createAudioUrl(String audioKey) {
        // audioKey가 없으면 URL 생성 불가
        if (audioKey == null || audioKey.isBlank()) {
            return null;
        }

        // 공백, 한글 등의 특수문자 인코딩 처리
        String encodedKey =
                UriUtils.encodePath(audioKey, StandardCharsets.UTF_8);

        // 도메인 + audioKey 조합
        return cloudFrontDomain + "/" + encodedKey;
    }

    // URL 중복 방지를 위해 마지막 "/" 제거
    private String normalizeDomain(String domain) {
        if (domain.endsWith("/")) {
            return domain.substring(0, domain.length() - 1);
        }

        return domain;
    }
}
