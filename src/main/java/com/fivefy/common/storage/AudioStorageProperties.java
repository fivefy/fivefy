package com.fivefy.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.audio")
public record AudioStorageProperties(
        String bucket,
        String region,
        String prefix
) {
    public String normalizedPrefix() {
        if (prefix == null || prefix.isBlank()) {
            return "tracks/audio";
        }

        return prefix.replaceAll("^/+", "").replaceAll("/+$", "");
    }
}
