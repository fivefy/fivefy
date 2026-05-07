package com.fivefy.ai.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class TrackLyricsEmbedding {

    private final Long trackId;
    private final float[] embedding;
    private final String snippet;          // 가사 첫 30자 (미리보기용)
    private final int chunkCount;
    private final String sourceHash;       // 원본 SHA-256
    private final LocalDateTime embeddedAt;
    private final String modelVersion;
}
