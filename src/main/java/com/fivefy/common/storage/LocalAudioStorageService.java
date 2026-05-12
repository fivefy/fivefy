package com.fivefy.common.storage;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.track.enums.TrackApplicationErrorCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "storage.audio.type", havingValue = "local")
public class LocalAudioStorageService implements AudioStorageService {

    private final AudioStorageProperties properties;

    public LocalAudioStorageService(AudioStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public String upload(MultipartFile audioFile) {
        validateAudioFile(audioFile);

        String audioKey = properties.normalizedPrefix() + "/" + UUID.randomUUID() + ".mp3";
        Path targetPath = Path.of(properties.normalizedLocalRoot()).resolve(audioKey);

        try {
            Files.createDirectories(targetPath.getParent());
            try (InputStream inputStream = audioFile.getInputStream()) {
                Files.copy(inputStream, targetPath);
            }
        } catch (IOException e) {
            throw new BusinessException(TrackApplicationErrorCode.ERR_AUDIO_UPLOAD_FAILED);
        }

        return audioKey;
    }

    @Override
    public void delete(String audioKey) {
        if (audioKey == null || audioKey.isBlank()) {
            return;
        }

        Path targetPath = Path.of(properties.normalizedLocalRoot()).resolve(audioKey);

        try {
            Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            throw new BusinessException(TrackApplicationErrorCode.ERR_AUDIO_UPLOAD_FAILED);
        }
    }

    static void validateAudioFile(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new BusinessException(TrackApplicationErrorCode.ERR_INVALID_AUDIO_FILE);
        }

        String filename = audioFile.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".mp3")) {
            throw new BusinessException(TrackApplicationErrorCode.ERR_INVALID_AUDIO_FILE);
        }

        String contentType = audioFile.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            String normalizedContentType = contentType.toLowerCase();
            boolean allowedContentType = normalizedContentType.equals("audio/mpeg")
                    || normalizedContentType.equals("audio/mp3")
                    || normalizedContentType.equals("application/octet-stream")
                    || normalizedContentType.startsWith("audio/");
            if (!allowedContentType) {
                throw new BusinessException(TrackApplicationErrorCode.ERR_INVALID_AUDIO_FILE);
            }
        }

        if (!hasMp3Signature(audioFile)) {
            throw new BusinessException(TrackApplicationErrorCode.ERR_INVALID_AUDIO_FILE);
        }
    }

    private static boolean hasMp3Signature(MultipartFile audioFile) {
        try (InputStream inputStream = audioFile.getInputStream()) {
            byte[] header = inputStream.readNBytes(3);
            if (header.length < 3) {
                return false;
            }

            boolean hasId3Header = header[0] == 'I' && header[1] == 'D' && header[2] == '3';
            boolean hasFrameSync = (header[0] & 0xFF) == 0xFF && (header[1] & 0xE0) == 0xE0;
            return hasId3Header || hasFrameSync;
        } catch (IOException e) {
            return false;
        }
    }
}
