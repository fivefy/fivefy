package com.fivefy.ai.service;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Maximal Marginal Relevance — 유사도와 다양성의 균형.
 *
 * 문제 의식:
 *  유저 벡터와 가장 가까운 K개를 그냥 뽑으면 → 같은 가수 같은 분위기 곡들로 도배됨.
 *  유저는 "내가 이미 좋아하는 것과 비슷하면서도 어느 정도 다양한 것"을 원함.
 *
 * 알고리즘:
 *   다음 곡 = argmax_t [ λ × sim(t, user) - (1-λ) × max_s∈Selected sim(t, s) ]
 *
 * λ:
 *   1.0 → 유사도만 (다양성 X) = 그냥 top-K
 *   0.0 → 다양성만 (관련성 X)
 *   0.7 → 균형 (실용적 기본값)
 *
 * 시간복잡도: O(K × N) — Map 인덱싱으로 selected 조회 O(1).
 *
 * 참고: Carbonell & Goldstein (1998), "The Use of MMR, Diversity-Based Reranking..."
 */
@Component
public class MmrReranker {

    private static final double DEFAULT_LAMBDA = 0.7;

    public List<Long> rerank(
            float[] userVector,
            List<Candidate> candidates,
            int finalCount) {
        return rerank(userVector, candidates, finalCount, DEFAULT_LAMBDA);
    }

    public List<Long> rerank(
            float[] userVector,
            List<Candidate> candidates,
            int finalCount,
            double lambda) {

        if (candidates.isEmpty()) return List.of();
        if (candidates.size() <= finalCount) {
            return candidates.stream().map(Candidate::trackId).toList();
        }

        // 후보 인덱스: id → Candidate (selected 조회를 O(1)로)
        Map<Long, Candidate> byId = candidates.stream()
                .collect(Collectors.toMap(Candidate::trackId, c -> c));

        // 사전 계산: user-candidate 유사도
        Map<Long, Float> simToUser = new HashMap<>(candidates.size());
        for (Candidate c : candidates) {
            simToUser.put(c.trackId(), cosineSim(userVector, c.vector()));
        }

        List<Long> selected = new ArrayList<>(finalCount);
        Set<Long> selectedSet = new HashSet<>();

        // 첫 곡: 가장 유사한 것
        Candidate first = candidates.stream()
                .max((a, b) -> Float.compare(simToUser.get(a.trackId()), simToUser.get(b.trackId())))
                .orElseThrow();
        selected.add(first.trackId());
        selectedSet.add(first.trackId());

        while (selected.size() < finalCount) {
            Candidate best = null;
            double bestScore = Double.NEGATIVE_INFINITY;

            for (Candidate c : candidates) {
                if (selectedSet.contains(c.trackId())) continue;

                // selected 와의 최대 유사도
                float maxSimToSelected = 0.0f;
                for (Long sid : selected) {
                    Candidate sc = byId.get(sid);  // O(1)
                    float s = cosineSim(c.vector(), sc.vector());
                    if (s > maxSimToSelected) maxSimToSelected = s;
                }

                double score = lambda * simToUser.get(c.trackId())
                        - (1 - lambda) * maxSimToSelected;

                if (score > bestScore) {
                    bestScore = score;
                    best = c;
                }
            }

            if (best == null) break;
            selected.add(best.trackId());
            selectedSet.add(best.trackId());
        }

        return selected;
    }

    /**
     * 코사인 유사도. 두 벡터가 모두 정규화돼있다면 그냥 내적과 동일.
     */
    private static float cosineSim(float[] a, float[] b) {
        float dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na  += a[i] * a[i];
            nb  += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0f;
        return (float) (dot / (Math.sqrt(na) * Math.sqrt(nb)));
    }

    public record Candidate(Long trackId, float[] vector) {}
}
