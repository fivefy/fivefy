package com.fivefy.ai.service;

import com.fivefy.ai.domain.TrackLyricsEmbedding;
import com.fivefy.ai.dto.etc.TrackLyricsForEmbedding;
import com.fivefy.common.enums.AlgorithmErrorCode;
import com.fivefy.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LyricsEmbeddingService {

    private final EmbeddingClient embeddingClient;
    private final LyricsEmbeddingPersistService lyricsEmbeddingPersistService;

    private static final int VECTOR_DIM = 1024;
    private static final double FIRST_CHUNK_BOOST = 1.3;
    private static final int MIN_LYRICS_LENGTH = 50;

    public boolean embedLyrics(TrackLyricsForEmbedding input) {
        if (input.lyrics() == null || input.lyrics().length() < MIN_LYRICS_LENGTH) {
            log.debug("Skip trackId={}, 가사 길이 부족", input.trackId());
            return false;
        }

        String hash = sha256(input.lyrics());

        // 변경 감지
        String existingHash = lyricsEmbeddingPersistService.getExistingHash(input.trackId());
        if (hash.equals(existingHash)) {
            log.debug("Skip trackId={}, 가사 변동 없음", input.trackId());
            return false;
        }

        // 1) 청크 분할
        List<String> chunks = input.toChunks();
        if (chunks.isEmpty()) return false;

        // 2) 청크별 임베딩 (배치 호출 — 가사 한 곡당 1번 요청)
        List<float[]> chunkVectors = embeddingClient.embedBatch(chunks);

        // 3) 가중 평균
        float[] aggregated = weightedAverage(chunkVectors);

        // 4) 정규화
        normalize(aggregated);

        // 5) 저장 (가사 원문은 저장 X, 발췌만)
        lyricsEmbeddingPersistService.save(TrackLyricsEmbedding.builder()
                .trackId(input.trackId())
                .embedding(aggregated)
                .snippet(input.toSnippet())
                .chunkCount(chunks.size())
                .sourceHash(hash)
                .embeddedAt(LocalDateTime.now())
                .modelVersion(embeddingClient.getModelVersion())
                .build());

        log.info("가사 임베딩 완료: trackId={}, chunks={}", input.trackId(), chunks.size());
        return true;
    }

    private float[] weightedAverage(List<float[]> chunkVectors) {
        float[] sum = new float[VECTOR_DIM];
        double totalWeight = 0;

        for (int i = 0; i < chunkVectors.size(); i++) {
            double weight = (i == 0) ? FIRST_CHUNK_BOOST : 1.0;
            float[] v = chunkVectors.get(i);
            for (int j = 0; j < VECTOR_DIM; j++) {
                sum[j] += (float) (weight * v[j]);
            }
            totalWeight += weight;
        }

        for (int j = 0; j < VECTOR_DIM; j++) {
            sum[j] /= (float) totalWeight;
        }
        return sum;
    }

    private void normalize(float[] v) {
        double sumSq = 0.0;
        for (float x : v) sumSq += x * x;
        double norm = Math.sqrt(sumSq);
        if (norm == 0.0) return;
        for (int i = 0; i < v.length; i++) v[i] /= (float) norm;
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(AlgorithmErrorCode.ERR_ALGORITHM_NOT_FOUND);
        }
    }
}
