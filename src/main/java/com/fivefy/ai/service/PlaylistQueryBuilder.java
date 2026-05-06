package com.fivefy.ai.service;

import com.fivefy.ai.enums.AiErrorCode;
import com.fivefy.ai.repository.TrackEmbeddingRepository;
import com.fivefy.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

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
            throw new BusinessException(AiErrorCode.ERR_AI_PLAYLIST_INVALID_INPUT);
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
            log.warn("시드 트랙 임베딩 조회 실패: trackIds={}", seedTrackIds);

            throw new BusinessException(AiErrorCode.ERR_AI_PLAYLIST_SEED_NOT_FOUND);
        }

        float[] sum = new float[VECTOR_DIM];
        for (float[] v : vectors.values()) {
            for (int i = 0; i < VECTOR_DIM; i++) sum[i] += v[i];
        }
        for (int i = 0; i < VECTOR_DIM; i++) sum[i] /= vectors.size();
        return sum;
    }

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
