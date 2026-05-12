package com.fivefy.domain.playback.dto.request;

import jakarta.validation.constraints.NotNull;

public record PlaybackStopRequest(
        @NotNull(message = "id는 필수입니다")
        Long id,

        @NotNull(message = "playedDuration은 필수입니다")
        Integer playedDuration
) {
}
