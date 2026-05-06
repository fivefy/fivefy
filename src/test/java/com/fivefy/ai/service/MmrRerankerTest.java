package com.fivefy.ai.service;

import com.fivefy.ai.dto.etc.Candidate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MmrRerankerTest {

    private final MmrReranker reranker = new MmrReranker();

    @Test
    @DisplayName("lambda=1.0이면 그냥 유저 벡터와 가장 비슷한 순서대로 뽑는다")
    void lambdaOneEqualsTopK() {
        float[] user = {1.0f, 0.0f, 0.0f};

        List<Candidate> candidates = List.of(
                new Candidate(1L, new float[]{0.9f, 0.1f, 0.0f}),  // 가장 유사
                new Candidate(2L, new float[]{0.8f, 0.2f, 0.0f}),
                new Candidate(3L, new float[]{0.5f, 0.5f, 0.0f}),
                new Candidate(4L, new float[]{0.0f, 1.0f, 0.0f})
        );

        List<Long> result = reranker.rerank(user, candidates, 3, 1.0);

        // lambda=1이면 다양성 무시 → 유사도 순서대로
        assertThat(result).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("lambda=0.3면 다양성을 고려해서 비슷한 곡을 연속으로 뽑지 않는다")
    void diversityIsApplied() {
        float[] user = {1.0f, 0.0f, 0.0f};

        // 1번과 2번은 거의 동일한 곡 (유사도 0.99) - 둘 다 뽑히면 안 됨
        // 3번은 약간 다른 곡 - 다양성 측면에서 더 좋음
        List<Candidate> candidates = List.of(
                new Candidate(1L, new float[]{0.94f, 0.05f, 0.0f}),
                new Candidate(2L, new float[]{0.93f, 0.07f, 0.0f}),  // 1번과 거의 같음
                new Candidate(3L, new float[]{0.5f, 0.6f, 0.0f})    // 다른 방향
        );

        List<Long> result = reranker.rerank(user, candidates, 2, 0.3);

        // 1번은 가장 유사하므로 무조건 첫 번째.
        // 두 번째는 1번과 너무 비슷한 2번보다 다양성을 주는 3번이 선택되어야 함.
        assertThat(result).containsExactly(1L, 3L);
    }

    @Test
    @DisplayName("후보 개수 ≤ 요청 개수면 모두 반환")
    void allCandidatesIfFewerThanRequested() {
        float[] user = {1.0f, 0.0f};
        List<Candidate> candidates = List.of(
                new Candidate(1L, new float[]{1.0f, 0.0f}),
                new Candidate(2L, new float[]{0.5f, 0.5f})
        );

        List<Long> result = reranker.rerank(user, candidates, 10);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("빈 후보 리스트는 빈 결과")
    void emptyCandidates() {
        List<Long> result = reranker.rerank(new float[]{1, 0}, List.of(), 5);
        assertThat(result).isEmpty();
    }
}