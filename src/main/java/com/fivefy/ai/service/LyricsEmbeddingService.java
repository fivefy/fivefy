package com.fivefy.ai.service;

import com.fivefy.ai.domain.TrackLyricsEmbedding;
import com.fivefy.ai.dto.TrackLyricsForEmbedding;
import com.fivefy.ai.repository.TrackLyricsEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

/**
 * 가사 임베딩 서비스.
 *
 * 핵심 알고리즘:
 *   1) 가사 → 청크 N개 분할 (TrackLyrics.toChunks)
 *   2) 청크별로 임베딩 호출 (batch)
 *   3) 가중 평균: 첫 청크(보통 후렴/도입부) 1.3배 가중
 *   4) 정규화 → 1024차원 벡터
 *
 *  "왜 source_text를 저장 안 하나?"
 *   → 가사는 KOMCA 관리 저작물. 평문 보관은 라이선스 이슈.
 *     벡터는 역산 불가능해서 안전.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LyricsEmbeddingService {

    private final EmbeddingClient embeddingClient;
    private final TrackLyricsEmbeddingRepository lyricsEmbeddingRepository;

    private static final int VECTOR_DIM = 1024;
    private static final double FIRST_CHUNK_BOOST = 1.3;  // 첫 청크 가중치
    private static final int MIN_LYRICS_LENGTH = 50;       // 너무 짧으면 무의미

    /**
     * 단일 트랙 가사 임베딩.
     */
    @Transactional(transactionManager = "vectorTransactionManager")
    public boolean embedLyrics(TrackLyricsForEmbedding input) {
        if (input.lyrics() == null || input.lyrics().length() < MIN_LYRICS_LENGTH) {
            log.debug("Skip trackId={}, lyrics too short", input.trackId());
            return false;
        }

        String hash = sha256(input.lyrics());

        // 변경 감지
        String existingHash = lyricsEmbeddingRepository.findHashByTrackId(input.trackId());
        if (hash.equals(existingHash)) {
            log.debug("Skip trackId={}, lyrics unchanged", input.trackId());
            return false;
        }

        // 1) 청크 분할
        List<String> chunks = input.toChunks();
        if (chunks.isEmpty()) return false;

        // 2) 청크별 임베딩 (배치 호출 — 가사 한 곡당 OpenAI 1번 요청)
        List<float[]> chunkVectors = embeddingClient.embedBatch(chunks);

        // 3) 가중 평균
        float[] aggregated = weightedAverage(chunkVectors);

        // 4) 정규화
        normalize(aggregated);

        // 5) 저장 (가사 원문은 저장 X, 발췌만)
        lyricsEmbeddingRepository.upsert(TrackLyricsEmbedding.builder()
                .trackId(input.trackId())
                .embedding(aggregated)
                .snippet(input.toSnippet())
                .chunkCount(chunks.size())
                .sourceHash(hash)
                .embeddedAt(LocalDateTime.now())
                .modelVersion(embeddingClient.getModelVersion())
                .build());

        log.info("Lyrics embedded: trackId={}, chunks={}", input.trackId(), chunks.size());
        return true;
    }

    /**
     * 청크 벡터들의 가중 평균.
     *
     * 가중치 분포:
     *   chunk[0]: FIRST_CHUNK_BOOST (1.3)
     *   chunk[1..N-1]: 1.0
     *
     * 후렴은 보통 곡 초반에 등장하거나 반복 등장하므로,
     * 첫 청크(주로 도입부 + 첫 후렴)에 살짝 가중치를 더 줌.
     *
     * 더 정교하게 하려면:
     *  - 가사 구조 파싱 (verse / chorus 인식)
     *  - 반복 라인 감지 후 후렴 추론
     *  - 하지만 ROI 낮음 — 단순 가중치로도 효과 충분
     */
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
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
