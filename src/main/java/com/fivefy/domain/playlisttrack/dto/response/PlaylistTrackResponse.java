package com.fivefy.domain.playlisttrack.dto.response;

import com.fivefy.domain.playlisttrack.entity.PlaylistTrack;

import java.time.LocalDateTime;

public record PlaylistTrackResponse(
        Long playlistTrackId,
        Long playlistId,
        Long trackId,
        Integer position,
        LocalDateTime createdAt
) {
    public static PlaylistTrackResponse from(PlaylistTrack playlistTrack) {
        return new PlaylistTrackResponse(
                playlistTrack.getId(),
                playlistTrack.getPlaylistId(),
                playlistTrack.getTrackId(),
                playlistTrack.getPosition(),
                playlistTrack.getCreatedAt()
        );
    }
}
