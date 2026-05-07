package com.fivefy.ai.dto.response;

public record MoodTrack(
        Long trackId,
        String title,
        String artist,
        String albumCoverUrl,
        float finalScore,
        float metaScore,
        float lyricsScore,
        boolean hasLyrics  // UI에서 가사 매칭 여부 표시 가능
) {
}
