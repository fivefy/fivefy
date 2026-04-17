package com.fivefy.domain.search.dto.response;

import com.fivefy.domain.track.entity.Track;

public record SearchTrackResponse(

        Long id,
        String title,
        String genre,
        Long durationSec
) {
    public static SearchTrackResponse from(Track track) {
        return new SearchTrackResponse(
                track.getId(),
                track.getTitle(),
                track.getGenre(),
                track.getDurationSec()
        );
    }
}