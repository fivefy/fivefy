package com.fivefy.domain.playback.dto.request;

import jakarta.validation.constraints.NotNull;

public record PlaybackSkipRequest(
        @NotNull(message = "id는 필수입니다")
        Long id
) {
}
