package com.fivefy.ai.service;

import com.fivefy.ai.dto.etc.Candidate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

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
}
