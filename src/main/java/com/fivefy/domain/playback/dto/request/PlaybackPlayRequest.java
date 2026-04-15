package com.fivefy.domain.playback.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlaybackPlayRequest(
        @NotNull(message = "playlistId는 필수입니다")
        Long playlistId,
        @NotNull(message = "trackId는 필수입니다")
        Long trackId,
        @NotBlank(message = "sessionId는 필수입니다")
        String sessionId,
        String deviceId
) {
}
