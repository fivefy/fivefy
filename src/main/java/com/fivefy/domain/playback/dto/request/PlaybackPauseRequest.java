package com.fivefy.domain.playback.dto.request;

import jakarta.validation.constraints.NotNull;

public record PlaybackPauseRequest(
        @NotNull(message = "id는 필수입니다")
        Long id
) {
}
