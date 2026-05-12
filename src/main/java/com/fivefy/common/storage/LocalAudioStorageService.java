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

    static void validateAudioFile(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new BusinessException(TrackApplicationErrorCode.ERR_INVALID_AUDIO_FILE);
        }

        String filename = audioFile.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".mp3")) {
            throw new BusinessException(TrackApplicationErrorCode.ERR_INVALID_AUDIO_FILE);
        }
    }
}
