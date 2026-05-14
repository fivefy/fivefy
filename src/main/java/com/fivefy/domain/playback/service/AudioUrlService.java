package com.fivefy.domain.playback.service;

import com.fivefy.common.storage.AudioStorageProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 저장된 audioKey를 실제 재생 가능한 audioUrl로 변환하는 서비스
 */
@Service
public class AudioUrlService {

    private static final Duration S3_PLAY_URL_TTL = Duration.ofMinutes(10);

    private final AudioStorageProperties audioStorageProperties;
    private final ObjectProvider<S3Presigner> s3PresignerProvider;
    private final String cloudFrontDomain;

    public AudioUrlService(
            AudioStorageProperties audioStorageProperties,
            ObjectProvider<S3Presigner> s3PresignerProvider,
            @Value("${cloudfront.audio-domain:}") String cloudFrontDomain
    ) {
        this.audioStorageProperties = audioStorageProperties;
        this.s3PresignerProvider = s3PresignerProvider;
        this.cloudFrontDomain = normalizeDomain(cloudFrontDomain);
    }

    /**
     * audioKey를 기반으로 실제 접근 가능한 audioUrl 생성
     */
    public String createAudioUrl(String audioKey) {
        if (audioKey == null || audioKey.isBlank()) {
            return null;
        }

        if ("s3".equalsIgnoreCase(audioStorageProperties.type())) {
            return createS3PresignedUrl(audioKey);
        }

        if ("local".equalsIgnoreCase(audioStorageProperties.type())) {
            return createLocalAudioUrl(audioKey);
        }

        if (cloudFrontDomain.isBlank()) {
            return null;
        }

        String encodedKey = UriUtils.encodePath(audioKey, StandardCharsets.UTF_8);
        return cloudFrontDomain + "/" + encodedKey;
    }

    private String createLocalAudioUrl(String audioKey) {
        String encodedKey = UriUtils.encodePath(audioKey, StandardCharsets.UTF_8);
        return audioStorageProperties.normalizedPublicBaseUrl() + "/" + encodedKey;
    }

    private String createS3PresignedUrl(String audioKey) {
        S3Presigner s3Presigner = s3PresignerProvider.getIfAvailable();
        if (s3Presigner == null) {
            return null;
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(audioStorageProperties.bucket())
                .key(audioKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(S3_PLAY_URL_TTL)
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    private String normalizeDomain(String domain) {
        if (domain == null || domain.isBlank()) {
            return "";
        }

        if (domain.endsWith("/")) {
            return domain.substring(0, domain.length() - 1);
        }

        return domain;
    }
}
