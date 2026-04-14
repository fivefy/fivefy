package com.fivefy.domain.playlist.dto.response;

import com.fivefy.domain.playlist.entity.Playlist;

import java.time.LocalDateTime;

public record PlaylistResponse(
        Long id,
        Long userId,
        String title,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {
    public static PlaylistResponse from(Playlist playlist) {
        return new PlaylistResponse(
                playlist.getId(),
                playlist.getUserId(),
                playlist.getTitle(),
                playlist.getDescription(),
                playlist.getCreatedAt(),
                playlist.getUpdatedAt(),
                playlist.getDeletedAt()
        );
    }
}
