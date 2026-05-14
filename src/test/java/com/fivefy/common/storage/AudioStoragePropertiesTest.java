package com.fivefy.common.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("오디오 저장 설정")
class AudioStoragePropertiesTest {

    @Test
    @DisplayName("prefix가 슬래시만 있으면 기본 오디오 경로를 사용한다")
    void normalizedPrefix_fallbacksToDefaultWhenBlankAfterTrim() {
        AudioStorageProperties properties = new AudioStorageProperties(
                "s3",
                "bucket",
                "ap-northeast-2",
                "/",
                null,
                null
        );

        assertThat(properties.normalizedPrefix()).isEqualTo("tracks/audio");
    }

    @Test
    @DisplayName("localRoot가 슬래시만 있으면 기본 로컬 저장 경로를 사용한다")
    void normalizedLocalRoot_fallbacksToDefaultWhenBlankAfterTrim() {
        AudioStorageProperties properties = new AudioStorageProperties(
                "local",
                null,
                null,
                "tracks/audio",
                "/",
                "http://localhost:8080"
        );

        assertThat(properties.normalizedLocalRoot()).isEqualTo("build/audio-storage");
    }
}
