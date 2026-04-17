package com.fivefy.domain.playback.dto.projection;

public record TrackPlayCountDto(
        Long trackId,
        Long playCount
) {
}
