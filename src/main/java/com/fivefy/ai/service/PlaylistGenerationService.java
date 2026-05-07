package com.fivefy.ai.service;

import com.fivefy.ai.dto.etc.Candidate;
import com.fivefy.ai.dto.request.PlaylistGenerateRequest;
import com.fivefy.ai.dto.response.PlaylistGenerateResponse;
import com.fivefy.ai.dto.etc.RawTrack;
import com.fivefy.ai.dto.response.RecommendationResponse;
import com.fivefy.ai.enums.AiErrorCode;
import com.fivefy.ai.repository.TrackEmbeddingRepository;
import com.fivefy.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaylistGenerationService {

    private final PromptToSearchTextService promptConverter;
    private final PlaylistQueryBuilder queryBuilder;
    private final TrackEmbeddingRepository trackEmbeddingRepository;
    private final MmrReranker mmrReranker;
    private final NamedParameterJdbcTemplate primaryNamedJdbcTemplate;

    private static final int CANDIDATE_MULTIPLIER = 3;
    private static final double DEFAULT_PLAYLIST_LAMBDA = 0.5;

    public PlaylistGenerateResponse generate(PlaylistGenerateRequest req) {
        if (!req.hasInput()) {
            throw new BusinessException(AiErrorCode.ERR_AI_PLAYLIST_INVALID_INPUT);
        }

        // ─── 1. prompt → 검색 텍스트 ───
        String searchText = null;
        if (req.prompt() != null && !req.prompt().isBlank()) {
            searchText = promptConverter.convert(req.prompt());
            log.debug("프롬프트 변환 완료: '{}' → '{}'", req.prompt(), searchText);
        }

        // ─── 2. 검색 벡터 빌드 ───
        float[] queryVector = queryBuilder.build(searchText, req.seedTrackIds());

        // ─── 3. ANN 검색 ───
        int k = req.size() * CANDIDATE_MULTIPLIER;
        // seed 트랙은 결과에서 제외
        List<Long> excludeIds = req.seedTrackIds() == null ? List.of() : req.seedTrackIds();

        List<Long> candidateIds = trackEmbeddingRepository.findSimilarTrackIds(
                queryVector, k, excludeIds);

        if (candidateIds.isEmpty()) {
            return new PlaylistGenerateResponse(searchText, List.of());
        }

        // ─── 4. MMR 재정렬 ───
        double lambda = req.diversityLambda() != null
                ? req.diversityLambda()
                : DEFAULT_PLAYLIST_LAMBDA;

        Map<Long, float[]> candidateVectors = trackEmbeddingRepository
                .findVectorsByTrackIds(candidateIds);

        List<Candidate> mmrInput = candidateIds.stream()
                .filter(candidateVectors::containsKey)
                .map(id -> new Candidate(id, candidateVectors.get(id)))
                .toList();

        List<Long> finalIds = mmrReranker.rerank(queryVector, mmrInput, req.size(), lambda);

        // ─── 5. 메타데이터 ───
        List<RecommendationResponse.RecommendedTrack> tracks = enrichWithMetadata(finalIds, queryVector, candidateVectors);

        // 이름/설명은 별도 메서드로 (자동 생성된 플리 저장 시 추가 LLM 호출)
        return new PlaylistGenerateResponse(searchText, tracks);
    }

    private List<RecommendationResponse.RecommendedTrack> enrichWithMetadata(
            List<Long> finalIds,
            float[] queryVector,
            Map<Long, float[]> vectors) {

        if (finalIds.isEmpty()) return List.of();

        String sql = """
            SELECT
                t.id,
                t.title,
                ar.name AS artist,
                al.cover_image_url AS cover
            FROM tracks t
            LEFT JOIN artists ar ON ar.id = t.artist_id
            LEFT JOIN albums  al ON al.id = t.album_id
            WHERE t.id IN (:ids)
            """;

        MapSqlParameterSource params = new MapSqlParameterSource("ids", finalIds);
        Map<Long, RawTrack> meta = new java.util.HashMap<>();
        primaryNamedJdbcTemplate.query(sql, params, (RowCallbackHandler) rs ->
                meta.put(rs.getLong("id"),
                        new RawTrack(
                                rs.getLong("id"),
                                rs.getString("title"),
                                rs.getString("artist"),
                                rs.getString("cover"))));

        List<RecommendationResponse.RecommendedTrack> result = new java.util.ArrayList<>(finalIds.size());
        for (Long id : finalIds) {
            RawTrack m = meta.get(id);
            if (m == null) continue;
            float score = cosineSim(queryVector, vectors.get(id));
            result.add(new RecommendationResponse.RecommendedTrack(m.id(), m.title(), m.artist(), m.cover(), score));
        }
        return result;
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
