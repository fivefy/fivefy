package com.fivefy.common.storage;

import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "storage.audio")
public record AudioStorageProperties(
        String type,
        String bucket,
        String region,
        String prefix,
        String localRoot,
        String publicBaseUrl
) {
    public String normalizedPrefix() {
        if (prefix == null || prefix.isBlank()) {
            return "tracks/audio";
        }

        String normalized = prefix.replaceAll("^/+", "").replaceAll("/+$", "");
        return normalized.isBlank() ? "tracks/audio" : normalized;
    }

    public String normalizedLocalRoot() {
        if (localRoot == null || localRoot.isBlank()) {
            return "build/audio-storage";
        }

        String normalized = localRoot.replaceAll("/+$", "");
        return normalized.isBlank() ? "build/audio-storage" : normalized;
    }

    public String normalizedPublicBaseUrl() {
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            return "http://localhost:8080";
        }

        String normalized = publicBaseUrl.replaceAll("/+$", "");
        return normalized.isBlank() ? "http://localhost:8080" : normalized;
    }

    @AssertTrue(message = "S3 오디오 저장소 bucket 설정은 필수입니다")
    public boolean isBucketConfiguredWhenUsingS3() {
        return type == null
                || !"s3".equalsIgnoreCase(type)
                || (bucket != null && !bucket.isBlank());
    }
}
