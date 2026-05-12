package com.fivefy.domain.playback.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AudioUrlServiceTest {

    private static final String CLOUD_FRONT_DOMAIN = "https://cdn.fivefy.com";

    private final AudioUrlService audioUrlService =
            new AudioUrlService(CLOUD_FRONT_DOMAIN);

    @Test
    @DisplayName("audioKey가 정상 값이면 CloudFront 기반 audioUrl을 생성한다")
    void createAudioUrlSuccess() {
        // given
        String audioKey = "tracks/test.mp3";

        // when
        String result = audioUrlService.createAudioUrl(audioKey);

        // then
        assertThat(result)
                .isEqualTo("https://cdn.fivefy.com/tracks/test.mp3");
    }

    @Test
    @DisplayName("audioKey가 null이면 null을 반환한다")
    void createAudioUrlNull() {
        // when
        String result = audioUrlService.createAudioUrl(null);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("audioKey가 공백이면 null을 반환한다")
    void createAudioUrlBlank() {
        // when
        String result = audioUrlService.createAudioUrl("   ");

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("audioKey가 빈 문자열이면 null을 반환한다")
    void createAudioUrlEmpty() {
        // when
        String result = audioUrlService.createAudioUrl("");

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("audioKey 앞뒤에 공백이 있어도 그대로 URL을 생성한다")
    void createAudioUrlWithSpaceInKey() {
        // given
        String audioKey = " tracks/test.mp3 ";

        // when
        String result = audioUrlService.createAudioUrl(audioKey);

        // then
        assertThat(result)
                .isEqualTo("https://cdn.fivefy.com/ tracks/test.mp3 ");
    }
}
