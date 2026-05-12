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

@DisplayName("로컬 오디오 저장소")
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
            MockMultipartFile audioFile = audioFile("sample.mp3", "audio/mpeg", validMp3Bytes());

            String audioKey = storageService.upload(audioFile);

            assertThat(audioKey).startsWith("tracks/audio/");
            assertThat(audioKey).endsWith(".mp3");
            assertThat(Files.readAllBytes(tempDir.resolve(audioKey))).isEqualTo(validMp3Bytes());
        }

        @Test
        @DisplayName("MP3 파일이 아니면 오디오 파일 저장 실패")
        void upload_fail_whenInvalidFile() {
            LocalAudioStorageService storageService = new LocalAudioStorageService(properties());
            MockMultipartFile audioFile = audioFile("sample.wav", "audio/wav", "audio".getBytes());

            assertThatThrownBy(() -> storageService.upload(audioFile))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackApplicationErrorCode.ERR_INVALID_AUDIO_FILE.getMessage());
        }

        @Test
        @DisplayName("MP3 확장자여도 오디오 Content-Type이 아니면 오디오 파일 저장 실패")
        void upload_fail_whenContentTypeInvalid() {
            LocalAudioStorageService storageService = new LocalAudioStorageService(properties());
            MockMultipartFile audioFile = audioFile("sample.mp3", "text/plain", validMp3Bytes());

            assertThatThrownBy(() -> storageService.upload(audioFile))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackApplicationErrorCode.ERR_INVALID_AUDIO_FILE.getMessage());
        }

        @Test
        @DisplayName("MP3 확장자와 Content-Type이 맞아도 헤더가 MP3가 아니면 오디오 파일 저장 실패")
        void upload_fail_whenHeaderInvalid() {
            LocalAudioStorageService storageService = new LocalAudioStorageService(properties());
            MockMultipartFile audioFile = audioFile("sample.mp3", "audio/mpeg", "not-mp3".getBytes());

            assertThatThrownBy(() -> storageService.upload(audioFile))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackApplicationErrorCode.ERR_INVALID_AUDIO_FILE.getMessage());
        }
    }

    @Nested
    @DisplayName("오디오 파일 삭제")
    class Delete {

        @Test
        @DisplayName("로컬 저장소는 audioKey 경로의 MP3 파일을 삭제한다")
        void delete_success() {
            LocalAudioStorageService storageService = new LocalAudioStorageService(properties());
            MockMultipartFile audioFile = audioFile("sample.mp3", "audio/mpeg", validMp3Bytes());
            String audioKey = storageService.upload(audioFile);

            storageService.delete(audioKey);

            assertThat(Files.exists(tempDir.resolve(audioKey))).isFalse();
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

    private byte[] validMp3Bytes() {
        return new byte[]{'I', 'D', '3', 0x04, 0x00, 0x00};
    }
}
