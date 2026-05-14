package com.fivefy.domain.playback.dto.response;

public record TrackPlayResponse(
        Long trackId,
        String audioUrl,
        Long playCount
) {
}
