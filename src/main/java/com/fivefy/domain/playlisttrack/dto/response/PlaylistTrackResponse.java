package com.fivefy.domain.playlisttrack.dto.response;

import com.fivefy.domain.playlisttrack.entity.PlaylistTrack;

public record PlaylistTrackResponse(
        Long playlistTrackId,
        Long playlistId,
        Long trackId,
        Integer position
) {
    public static PlaylistTrackResponse from(PlaylistTrack playlistTrack) {
        return new PlaylistTrackResponse(
                playlistTrack.getId(),
                playlistTrack.getPlaylistId(),
                playlistTrack.getTrackId(),
                playlistTrack.getPosition()
        );
    }
}
