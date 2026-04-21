package com.fivefy.domain.album.dto.response;

import com.fivefy.domain.track.entity.Track;

/**
 * 앨범 수록곡 응답 DTO
 */
public record AlbumTrackResponse(
        Long trackId,
        Long trackNumber,
        String title,
        Long durationSec
) {
    public static AlbumTrackResponse from(Track track) {
        return new AlbumTrackResponse(
                track.getId(),
                track.getTrackNumber(),
                track.getTitle(),
                track.getDurationSec()
        );
    }
}