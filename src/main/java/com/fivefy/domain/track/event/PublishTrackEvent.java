package com.fivefy.domain.track.event;

public record PublishTrackEvent(
        Long artistId,
        Long trackId,
        String trackTitle
) {
    public static PublishTrackEvent of(Long artistId, Long trackId, String trackTitle) {
        return new PublishTrackEvent(artistId, trackId, trackTitle);
    }
}
