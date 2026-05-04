package com.fivefy.ai.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * 유저 임베딩 - 유저의 음악 취향을 벡터로 표현.
 *
 * 1차 구현: 최근 들은 N곡 임베딩의 (가중) 평균
 * basedOnCount: 신뢰도 지표로 사용 (적으면 추천 품질 낮음)
 */
@Getter
@Builder
@RequiredArgsConstructor
public class UserEmbedding {

    private final Long userId;
    private final float[] embedding;
    private final int basedOnCount;
    private final LocalDateTime computedAt;
}
