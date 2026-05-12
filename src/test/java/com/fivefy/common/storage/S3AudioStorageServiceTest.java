package com.fivefy.common.storage;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.track.enums.TrackApplicationErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockMultipartFile;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3AudioStorageService")
class S3AudioStorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Nested
    @DisplayName("오디오 파일 업로드")
    class Upload {

        @Test
        @DisplayName("S3 저장소는 MP3 파일을 업로드하고 audioKey를 반환한다")
        void upload_success() {
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());
            S3AudioStorageService storageService = new S3AudioStorageService(s3Client, properties());
            MockMultipartFile audioFile = audioFile("sample.mp3", null, "audio".getBytes());

            String audioKey = storageService.upload(audioFile);

            ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
            verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

            PutObjectRequest request = requestCaptor.getValue();
            assertThat(audioKey).startsWith("tracks/audio/");
            assertThat(audioKey).endsWith(".mp3");
            assertThat(request.bucket()).isEqualTo("fivefy-audio");
            assertThat(request.key()).isEqualTo(audioKey);
            assertThat(request.contentType()).isEqualTo("audio/mpeg");
            assertThat(request.contentLength()).isEqualTo(5L);
        }

        @Test
        @DisplayName("S3 업로드 실패 시 오디오 업로드 실패 예외를 반환한다")
        void upload_fail_whenS3UploadFails() {
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenThrow(S3Exception.builder().message("upload failed").build());
            S3AudioStorageService storageService = new S3AudioStorageService(s3Client, properties());
            MockMultipartFile audioFile = audioFile("sample.mp3", "audio/mpeg", "audio".getBytes());

            assertThatThrownBy(() -> storageService.upload(audioFile))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackApplicationErrorCode.ERR_AUDIO_UPLOAD_FAILED.getMessage());
        }
    }

    private AudioStorageProperties properties() {
        return new AudioStorageProperties(
                "s3",
                "fivefy-audio",
                "ap-northeast-2",
                "tracks/audio",
                null
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
