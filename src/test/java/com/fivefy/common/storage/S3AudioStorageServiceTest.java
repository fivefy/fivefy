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
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3 오디오 저장소")
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
            MockMultipartFile audioFile = audioFile("sample.mp3", null, validMp3Bytes());

            String audioKey = storageService.upload(audioFile);

            ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
            verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

            PutObjectRequest request = requestCaptor.getValue();
            assertThat(audioKey).startsWith("tracks/audio/");
            assertThat(audioKey).endsWith(".mp3");
            assertThat(request.bucket()).isEqualTo("fivefy-audio");
            assertThat(request.key()).isEqualTo(audioKey);
            assertThat(request.contentType()).isEqualTo("audio/mpeg");
            assertThat(request.contentLength()).isEqualTo(validMp3Bytes().length);
        }

        @Test
        @DisplayName("S3 업로드 실패 시 오디오 업로드 실패 예외를 반환한다")
        void upload_fail_whenS3UploadFails() {
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenThrow(S3Exception.builder().message("업로드 실패").build());
            S3AudioStorageService storageService = new S3AudioStorageService(s3Client, properties());
            MockMultipartFile audioFile = audioFile("sample.mp3", "audio/mpeg", validMp3Bytes());

            assertThatThrownBy(() -> storageService.upload(audioFile))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TrackApplicationErrorCode.ERR_AUDIO_UPLOAD_FAILED.getMessage());
        }
    }

    @Nested
    @DisplayName("오디오 파일 삭제")
    class Delete {

        @Test
        @DisplayName("S3 저장소는 audioKey 경로의 MP3 파일을 삭제한다")
        void delete_success() {
            S3AudioStorageService storageService = new S3AudioStorageService(s3Client, properties());

            storageService.delete("tracks/audio/test.mp3");

            ArgumentCaptor<DeleteObjectRequest> requestCaptor =
                    ArgumentCaptor.forClass(DeleteObjectRequest.class);
            verify(s3Client).deleteObject(requestCaptor.capture());

            DeleteObjectRequest request = requestCaptor.getValue();
            assertThat(request.bucket()).isEqualTo("fivefy-audio");
            assertThat(request.key()).isEqualTo("tracks/audio/test.mp3");
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

    private byte[] validMp3Bytes() {
        return new byte[]{'I', 'D', '3', 0x04, 0x00, 0x00};
    }
}
