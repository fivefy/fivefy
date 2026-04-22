package com.fivefy.domain.track.event;

import java.util.List;

public record PublishTrackChunkEvent(
        Long artistId,
        Long trackId,
        String trackTitle,
        Long chunkIndex,
        List<Long> userIds
) {
    public static PublishTrackChunkEvent of(
            Long artistId, Long trackId,
            String trackTitle,Long chunkIndex, List<Long> userIds) {
        return new PublishTrackChunkEvent(artistId, trackId, trackTitle,chunkIndex, userIds);
    }
}
