package com.fivefy.ai.service;

import com.fivefy.ai.dto.ScoredResult;
import com.fivefy.ai.repository.TrackEmbeddingRepository;
import com.fivefy.ai.repository.TrackLyricsEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 하이브리드 의미 검색 — 메타 임베딩 + 가사 임베딩 결합.
 *
 * 알고리즘:
 *   final_score = α × meta_similarity + (1 - α) × lyrics_similarity
 *
 *   α = 0.6 (기본값):
 *     메타가 약간 더 비중. 장르/아티스트 일관성 ↑
 *
 *   α = 0.3 (감정 검색 모드):
 *     가사 비중. "이별 후 듣는 곡" 같은 감정 검색에 유리
 *
 *   α = 0.9 (장르 검색 모드):
 *     메타 비중. "K-pop 댄스 곡" 같은 검색에 유리
 *
 * 호출 방식:
 *  - 1차: 메타 검색으로 후보 K개 (빠름)
 *  - 2차: 그 K개에 한해 가사 점수 조회 (in-memory join)
 *  - 3차: 점수 결합 후 재정렬
 *
 * 왜 두 번 검색해서 결합 안 하나?
 *  - 두 인덱스에 별도 검색 → 각자 다른 곡 N개 → 합집합이 너무 커짐
 *  - 가사 임베딩 없는 곡은 가사 점수 0으로 처리 → 메타만 좋은 곡도 배제 안 됨
 *  - 메타 검색을 1차로 잡으면 "메타 OK + 가사 보너스" 구조 → 결과 안정
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridSemanticSearchService {

    private final TrackEmbeddingRepository trackEmbeddingRepository;
    private final TrackLyricsEmbeddingRepository lyricsEmbeddingRepository;

    private static final double DEFAULT_ALPHA = 0.6;
    private static final int CANDIDATE_MULTIPLIER = 4;

    public List<ScoredResult> search(float[] queryVector, int limit) {
        return search(queryVector, limit, DEFAULT_ALPHA);
    }

    public List<ScoredResult> search(float[] queryVector, int limit, double alpha) {
        // 1) 메타 검색으로 후보 K개
        int candidateK = limit * CANDIDATE_MULTIPLIER;
        List<Long> candidateIds = trackEmbeddingRepository.findSimilarTrackIds(queryVector, candidateK, List.of());
        if (candidateIds.isEmpty()) return List.of();

        // 2) 메타 점수 계산 (벡터 다시 가져와서 직접 계산)
        Map<Long, float[]> metaVectors = trackEmbeddingRepository.findVectorsByTrackIds(candidateIds);
        Map<Long, Float> metaScores = new HashMap<>();
        for (Map.Entry<Long, float[]> e : metaVectors.entrySet()) {
            metaScores.put(e.getKey(), cosineSim(queryVector, e.getValue()));
        }

        // 3) 가사 점수 (해당 후보들에 한해서만)
        Map<Long, Float> lyricsScores = lyricsEmbeddingRepository.getSimilarityScoresFor(
                queryVector, candidateIds);

        // 4) 점수 결합
        List<ScoredResult> combined = new ArrayList<>(candidateIds.size());
        for (Long id : candidateIds) {
            float metaScore = metaScores.getOrDefault(id, 0f);
            // 가사 임베딩이 없는 트랙은 가사 점수 0으로 (메타만으로 평가)
            Float lyricsScore = lyricsScores.get(id);
            boolean hasLyrics = lyricsScore != null;
            float ls = hasLyrics ? lyricsScore : 0f;

            // 가사 없는 곡은 alpha=1로 취급 (메타만)
            double effectiveAlpha = hasLyrics ? alpha : 1.0;
            float finalScore = (float) (effectiveAlpha * metaScore + (1 - effectiveAlpha) * ls);

            combined.add(new ScoredResult(id, finalScore, metaScore, ls, hasLyrics));
        }

        // 5) 점수 내림차순 정렬 후 상위 limit
        combined.sort((a, b) -> Float.compare(b.finalScore(), a.finalScore()));
        return combined.subList(0, Math.min(limit, combined.size()));
    }

    private static float cosineSim(float[] a, float[] b) {
        float dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0f;
        return (float) (dot / (Math.sqrt(na) * Math.sqrt(nb)));
    }
}
