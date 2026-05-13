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
    @DisplayName("audioKey 앞뒤에 공백이 있으면 인코딩된 URL을 생성한다")
    void createAudioUrlWithSpaceInKey() {
        // given
        String audioKey = " tracks/test.mp3 ";

        // when
        String result = audioUrlService.createAudioUrl(audioKey);

        // then
        assertThat(result)
                .isEqualTo("https://cdn.fivefy.com/%20tracks/test.mp3%20");
    }

    @Test
    @DisplayName("CloudFront 도메인 끝에 슬래시가 없어도 정상적으로 audioUrl을 생성한다")
    void createAudioUrlWithoutTrailingSlash() {
        // given
        AudioUrlService service =
                new AudioUrlService("https://cdn.fivefy.com");

        String audioKey = "tracks/test.mp3";

        // when
        String result = service.createAudioUrl(audioKey);

        // then
        assertThat(result)
                .isEqualTo("https://cdn.fivefy.com/tracks/test.mp3");
    }

    @Test
    @DisplayName("CloudFront 도메인 끝에 슬래시가 있으면 제거 후 audioUrl을 생성한다")
    void createAudioUrlWithTrailingSlashDomain() {
        // given
        AudioUrlService service =
                new AudioUrlService("https://cdn.fivefy.com/");

        String audioKey = "tracks/test.mp3";

        // when
        String result = service.createAudioUrl(audioKey);

        // then
        assertThat(result)
                .isEqualTo("https://cdn.fivefy.com/tracks/test.mp3");
    }
}
