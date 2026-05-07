package com.fivefy.ai.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class TrackEmbedding {

    private final Long trackId;
    private final float[] embedding;       // 1024차원
    private final String sourceText;       // 임베딩 원본 텍스트
    private final String sourceHash;       // SHA-256, 변경 감지용
    private final LocalDateTime embeddedAt;
    private final String modelVersion;
}
