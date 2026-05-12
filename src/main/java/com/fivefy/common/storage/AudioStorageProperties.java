package com.fivefy.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.audio")
public record AudioStorageProperties(
        String type,
        String bucket,
        String region,
        String prefix,
        String localRoot
) {
    public String normalizedPrefix() {
        if (prefix == null || prefix.isBlank()) {
            return "tracks/audio";
        }

        return prefix.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    public String normalizedLocalRoot() {
        if (localRoot == null || localRoot.isBlank()) {
            return "build/audio-storage";
        }

        return localRoot.replaceAll("/+$", "");
    }
}
