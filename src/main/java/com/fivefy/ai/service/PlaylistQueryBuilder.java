package com.fivefy.ai.service;

import com.fivefy.ai.repository.TrackEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 플레이리스트 검색용 쿼리 벡터를 만든다.
 *
 * 입력 조합:
 *  1) prompt만 있음          → prompt 벡터
 *  2) seed 트랙만 있음        → seed 트랙들 평균 벡터
 *  3) 둘 다 있음              → 가중 결합 (prompt α + seed (1-α))
 *  4) 둘 다 없음              → 호출자가 미리 검증, 여긴 안 옴
 *
 * α (alpha) = 0.6:
 *   prompt가 사용자의 *현재 의도*를 더 직접적으로 표현하니 살짝 더 무게.
 *   seed는 *유사한 트랙 찾기*의 닻 역할.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaylistQueryBuilder {

    private final EmbeddingClient embeddingClient;
    private final TrackEmbeddingRepository trackEmbeddingRepository;

    private static final int VECTOR_DIM = 1024;
    private static final double DEFAULT_PROMPT_WEIGHT = 0.6;

    public float[] build(String searchText, List<Long> seedTrackIds) {
        boolean hasPrompt = searchText != null && !searchText.isBlank();
        boolean hasSeed = seedTrackIds != null && !seedTrackIds.isEmpty();

        if (!hasPrompt && !hasSeed) {
            throw new IllegalArgumentException("Either prompt or seed tracks required");
        }

        if (hasPrompt && !hasSeed) {
            float[] v = embeddingClient.embed(searchText);
            normalize(v);
            return v;
        }

        if (!hasPrompt) {
            float[] v = averageSeedVectors(seedTrackIds);
            normalize(v);
            return v;
        }

        // ─── 둘 다 있음: 가중 결합 ───
        float[] promptVec = embeddingClient.embed(searchText);
        float[] seedVec = averageSeedVectors(seedTrackIds);

        return weightedSum(promptVec, seedVec, DEFAULT_PROMPT_WEIGHT);
    }

    private float[] averageSeedVectors(List<Long> seedTrackIds) {
        Map<Long, float[]> vectors = trackEmbeddingRepository.findVectorsByTrackIds(seedTrackIds);
        if (vectors.isEmpty()) {
            throw new IllegalArgumentException(
                    "No embeddings found for seed tracks: " + seedTrackIds);
        }

        float[] sum = new float[VECTOR_DIM];
        for (float[] v : vectors.values()) {
            for (int i = 0; i < VECTOR_DIM; i++) sum[i] += v[i];
        }
        for (int i = 0; i < VECTOR_DIM; i++) sum[i] /= vectors.size();
        return sum;
    }

    /**
     * 두 벡터의 가중 합 후 정규화.
     */
    private float[] weightedSum(float[] a, float[] b, double weightA) {
        float wa = (float) weightA;
        float wb = (float) (1.0 - weightA);

        float[] result = new float[VECTOR_DIM];
        for (int i = 0; i < VECTOR_DIM; i++) {
            result[i] = wa * a[i] + wb * b[i];
        }
        normalize(result);
        return result;
    }

    private void normalize(float[] v) {
        double sumSq = 0.0;
        for (float x : v) sumSq += x * x;
        double norm = Math.sqrt(sumSq);
        if (norm == 0.0) return;
        for (int i = 0; i < v.length; i++) v[i] /= (float) norm;
    }
}
