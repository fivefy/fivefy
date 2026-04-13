package com.fivefy.domain.playlist.dto.response;

import com.fivefy.domain.playlist.entity.Playlist;

import java.time.LocalDateTime;

public record PlaylistDeleteResponse(

        Long id,
        LocalDateTime deletedAt
) {
    public static PlaylistDeleteResponse from(Playlist playlist) {
        return new PlaylistDeleteResponse(
                playlist.getId(),
                playlist.getDeletedAt()
        );
    }
}
