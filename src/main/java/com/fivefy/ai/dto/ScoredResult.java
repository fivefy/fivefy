package com.fivefy.ai.dto;

public record ScoredResult(
        Long trackId,
        float finalScore,
        float metaScore,
        float lyricsScore,
        boolean hasLyricsEmbedding
) {
}
