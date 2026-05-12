package com.fivefy.common.storage;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.track.enums.TrackApplicationErrorCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@ConditionalOnMissingBean(AudioStorageService.class)
public class LocalAudioStorageService implements AudioStorageService {

    private final AudioStorageProperties properties;

    public LocalAudioStorageService(AudioStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public String upload(MultipartFile audioFile) {
        validateAudioFile(audioFile);

        return properties.normalizedPrefix() + "/" + UUID.randomUUID() + ".mp3";
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
