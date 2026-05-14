package com.fivefy.domain.playback.service;

import com.fivefy.common.storage.AudioStorageProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AudioUrlServiceTest {

    private static final String CLOUD_FRONT_DOMAIN = "https://cdn.fivefy.com";

    @Test
    @DisplayName("s3 저장소이면 presigned GET URL을 생성한다")
    void createAudioUrlSuccess_s3() {
        // given
        S3Presigner s3Presigner = S3Presigner.builder()
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create("test-access-key", "test-secret-key")
                        )
                )
                .build();

        AudioUrlService audioUrlService = createService(
                new AudioStorageProperties(
                        "s3",
                        "fivefy-audio",
                        "ap-northeast-2",
                        "tracks/audio",
                        null,
                        null
                ),
                s3Presigner,
                CLOUD_FRONT_DOMAIN
        );

        String audioKey = "tracks/audio/test.mp3";

        // when
        String result = audioUrlService.createAudioUrl(audioKey);

        // then
        assertThat(result).startsWith("https://fivefy-audio.s3.ap-northeast-2.amazonaws.com/tracks/audio/test.mp3?");
        assertThat(result).contains("X-Amz-Algorithm=AWS4-HMAC-SHA256");
        assertThat(result).contains("X-Amz-SignedHeaders=host");
    }

    @Test
    @DisplayName("local 저장소이면 publicBaseUrl 기반 audioUrl을 생성한다")
    void createAudioUrlSuccess_local() {
        // given
        AudioUrlService audioUrlService = createService(
                new AudioStorageProperties(
                        "local",
                        null,
                        null,
                        "tracks/audio",
                        "build/audio-storage",
                        "http://localhost:8080"
                ),
                null,
                CLOUD_FRONT_DOMAIN
        );

        String audioKey = "tracks/audio/test.mp3";

        // when
        String result = audioUrlService.createAudioUrl(audioKey);

        // then
        assertThat(result).isEqualTo("http://localhost:8080/tracks/audio/test.mp3");
    }

    @Test
    @DisplayName("audioKey가 null이면 null을 반환한다")
    void createAudioUrlNull() {
        // given
        AudioUrlService audioUrlService = createLocalService();

        // when
        String result = audioUrlService.createAudioUrl(null);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("audioKey가 공백이면 null을 반환한다")
    void createAudioUrlBlank() {
        // given
        AudioUrlService audioUrlService = createLocalService();

        // when
        String result = audioUrlService.createAudioUrl("   ");

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("audioKey가 빈 문자열이면 null을 반환한다")
    void createAudioUrlEmpty() {
        // given
        AudioUrlService audioUrlService = createLocalService();

        // when
        String result = audioUrlService.createAudioUrl("");

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("audioKey 앞뒤에 공백이 있으면 인코딩된 URL을 생성한다")
    void createAudioUrlWithSpaceInKey() {
        // given
        AudioUrlService audioUrlService = createLocalService();

        String audioKey = " tracks/audio/test.mp3 ";

        // when
        String result = audioUrlService.createAudioUrl(audioKey);

        // then
        assertThat(result)
                .isEqualTo("http://localhost:8080/%20tracks/audio/test.mp3%20");
    }

    @Test
    @DisplayName("local 저장소 publicBaseUrl 끝에 슬래시가 있으면 제거 후 audioUrl을 생성한다")
    void createAudioUrlWithTrailingSlashPublicBaseUrl() {
        // given
        AudioUrlService audioUrlService = createService(
                new AudioStorageProperties(
                        "local",
                        null,
                        null,
                        "tracks/audio",
                        "build/audio-storage",
                        "http://localhost:8080/"
                ),
                null,
                CLOUD_FRONT_DOMAIN
        );

        String audioKey = "tracks/audio/test.mp3";

        // when
        String result = audioUrlService.createAudioUrl(audioKey);

        // then
        assertThat(result)
                .isEqualTo("http://localhost:8080/tracks/audio/test.mp3");
    }

    @Test
    @DisplayName("알 수 없는 저장소 타입이면 CloudFront 기반 audioUrl을 생성한다")
    void createAudioUrlFallbackCloudFront() {
        // given
        AudioUrlService audioUrlService = createService(
                new AudioStorageProperties(
                        "cloudfront",
                        null,
                        null,
                        "tracks/audio",
                        "build/audio-storage",
                        null
                ),
                null,
                CLOUD_FRONT_DOMAIN
        );

        String audioKey = "tracks/audio/test.mp3";

        // when
        String result = audioUrlService.createAudioUrl(audioKey);

        // then
        assertThat(result)
                .isEqualTo("https://cdn.fivefy.com/tracks/audio/test.mp3");
    }

    @Test
    @DisplayName("CloudFront 도메인 끝에 슬래시가 있으면 제거 후 audioUrl을 생성한다")
    void createAudioUrlWithTrailingSlashDomain() {
        // given
        AudioUrlService audioUrlService = createService(
                new AudioStorageProperties(
                        "cloudfront",
                        null,
                        null,
                        "tracks/audio",
                        "build/audio-storage",
                        null
                ),
                null,
                "https://cdn.fivefy.com/"
        );

        String audioKey = "tracks/audio/test.mp3";

        // when
        String result = audioUrlService.createAudioUrl(audioKey);

        // then
        assertThat(result)
                .isEqualTo("https://cdn.fivefy.com/tracks/audio/test.mp3");
    }

    private AudioUrlService createLocalService() {
        return createService(
                new AudioStorageProperties(
                        "local",
                        null,
                        null,
                        "tracks/audio",
                        "build/audio-storage",
                        "http://localhost:8080"
                ),
                null,
                CLOUD_FRONT_DOMAIN
        );
    }

    private AudioUrlService createService(
            AudioStorageProperties properties,
            S3Presigner s3Presigner,
            String cloudFrontDomain
    ) {
        ObjectProvider<S3Presigner> s3PresignerProvider = mock(ObjectProvider.class);
        when(s3PresignerProvider.getIfAvailable()).thenReturn(s3Presigner);

        return new AudioUrlService(
                properties,
                s3PresignerProvider,
                cloudFrontDomain
        );
    }
}