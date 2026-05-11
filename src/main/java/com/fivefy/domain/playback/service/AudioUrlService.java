package com.fivefy.domain.playback.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AudioUrlService {

    private final String cloudFrontDomain;

    public AudioUrlService(
            @Value("${cloudfront.audio-domain}") String cloudFrontDomain
    ) {
        this.cloudFrontDomain = cloudFrontDomain;
    }

    public String createAudioUrl(String audioKey) {
        if (audioKey == null || audioKey.isBlank()) {
            return null;
        }

        return cloudFrontDomain + "/" + audioKey;
    }
}
