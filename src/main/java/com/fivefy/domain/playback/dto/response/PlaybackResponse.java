package com.fivefy.domain.playback.dto.response;

import com.fivefy.domain.playback.entity.Playback;
import com.fivefy.domain.playback.enums.PlaybackStatus;

import java.time.LocalDateTime;

public record PlaybackResponse(
        Long id,
        Long playlistId,
        Long trackId,
        Long userId,
        String sessionId,
        String deviceId,
        PlaybackStatus status,
        Integer playedDuration,
        LocalDateTime startedAt,
        LocalDateTime lastPlayedAt,
        LocalDateTime endedAt,
        String audioUrl
) {
    public static PlaybackResponse from(Playback playback) {
        return from(playback, null);
    }

    public static PlaybackResponse from(Playback playback, String audioUrl) {
        return new PlaybackResponse(
                playback.getId(),
                playback.getPlaylistId(),
                playback.getTrackId(),
                playback.getUserId(),
                playback.getSessionId(),
                playback.getDeviceId(),
                playback.getStatus(),
                playback.getPlayedDuration(),
                playback.getStartedAt(),
                playback.getLastPlayedAt(),
                playback.getEndedAt(),
                audioUrl
        );
    }
}
