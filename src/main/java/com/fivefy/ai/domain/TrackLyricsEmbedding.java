package com.fivefy.ai.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * 가사 임베딩 — 메타데이터 임베딩과 별도 테이블.
 *
 * source_text 필드가 없는 이유: 저작권.
 * 가사 원문을 평문으로 보관하지 않음. 원문은 라이선스 시스템에서만.
 */
@Getter
@Builder
@RequiredArgsConstructor
public class TrackLyricsEmbedding {

    private final Long trackId;
    private final float[] embedding;
    private final String snippet;          // 가사 첫 30자 (미리보기용)
    private final int chunkCount;
    private final String sourceHash;       // 원본 SHA-256
    private final LocalDateTime embeddedAt;
    private final String modelVersion;
}
