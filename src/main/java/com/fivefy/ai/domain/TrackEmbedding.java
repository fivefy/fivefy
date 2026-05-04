package com.fivefy.ai.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * 트랙 임베딩 (POJO, JPA 엔티티 X).
 * pgvector의 VECTOR 타입 때문에 JdbcTemplate으로 직접 다룬다.
 */
@Getter
@Builder
@RequiredArgsConstructor
public class TrackEmbedding {

    private final Long trackId;
    private final float[] embedding;       // 1024차원
    private final String sourceText;       // 임베딩 원본 텍스트
    private final String sourceHash;       // SHA-256, 변경 감지용
    private final LocalDateTime embeddedAt;
    private final String modelVersion;
}
