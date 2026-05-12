package com.fivefy.common.storage;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.track.enums.TrackApplicationErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LocalAudioStorageService")
class LocalAudioStorageServiceTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("오디오 파일 업로드")
    class Upload {

        @Test
        @DisplayName("로컬 저장소는 audioKey 경로에 MP3 파일을 저장한다")
        void upload_success() throws Exception {
            LocalAudioStorageService storageService = new LocalAudioStorageService(properties());
            MockMultipartFile audioFile = audioFile("sample.mp3", "audio/mpeg", "audio".getBytes());

            String audioKey = storageService.upload(audioFile);

            assertThat(audioKey).startsWith("tracks/audio/");
            assertThat(audioKey).endsWith(".mp3");
            assertThat(Files.readAllBytes(tempDir.resolve(audioKey))).isEqualTo("audio".getBytes());
        }

        @Test
        @DisplayName("MP3 파일이 아니면 로컬 저장을 실패한다")
        void upload_fail_whenInvalidFile() {
            LocalAudioStorageService storageService = new LocalAudioStorageService(properties());
            MockMultipartFile audioFile = audioFile("sample.wav", "audio/wav", "audio".getBytes());

            assertThatThrownBy(() -> storageService.upload(audioFile))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackApplicationErrorCode.ERR_INVALID_AUDIO_FILE.getMessage());
        }
    }

    private AudioStorageProperties properties() {
        return new AudioStorageProperties(
                "local",
                null,
                null,
                "tracks/audio",
                tempDir.toString()
        );
    }

    private MockMultipartFile audioFile(String filename, String contentType, byte[] content) {
        return new MockMultipartFile(
                "audioFile",
                filename,
                contentType,
                content
        );
    }
}
