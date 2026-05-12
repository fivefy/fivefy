package com.fivefy.common.storage;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.track.enums.TrackApplicationErrorCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "storage.audio.type", havingValue = "s3")
public class S3AudioStorageService implements AudioStorageService {

    private static final String AUDIO_MPEG_CONTENT_TYPE = "audio/mpeg";

    private final S3Client s3Client;
    private final AudioStorageProperties properties;

    public S3AudioStorageService(S3Client s3Client, AudioStorageProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    @Override
    public String upload(MultipartFile audioFile) {
        LocalAudioStorageService.validateAudioFile(audioFile);

        String audioKey = properties.normalizedPrefix() + "/" + UUID.randomUUID() + ".mp3";
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(audioKey)
                .contentType(resolveContentType(audioFile))
                .contentLength(audioFile.getSize())
                .build();

        try (InputStream inputStream = audioFile.getInputStream()) {
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, audioFile.getSize()));
        } catch (IOException | S3Exception | SdkClientException e) {
            throw new BusinessException(TrackApplicationErrorCode.ERR_AUDIO_UPLOAD_FAILED);
        }

        return audioKey;
    }

    @Override
    public void delete(String audioKey) {
        if (audioKey == null || audioKey.isBlank()) {
            return;
        }

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(properties.bucket())
                .key(audioKey)
                .build();

        try {
            s3Client.deleteObject(request);
        } catch (S3Exception | SdkClientException e) {
            throw new BusinessException(TrackApplicationErrorCode.ERR_AUDIO_UPLOAD_FAILED);
        }
    }

    private String resolveContentType(MultipartFile audioFile) {
        if (audioFile.getContentType() == null || audioFile.getContentType().isBlank()) {
            return AUDIO_MPEG_CONTENT_TYPE;
        }

        return audioFile.getContentType();
    }
}
