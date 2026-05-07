package com.fivefy.domain.chat.service;

import com.fivefy.ai.dto.etc.Candidate;
import com.fivefy.ai.dto.etc.RetrievedTrack;
import com.fivefy.ai.repository.TrackEmbeddingRepository;
import com.fivefy.ai.service.EmbeddingClient;
import com.fivefy.ai.service.MmrReranker;
import com.fivefy.ai.service.PromptToSearchTextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRetrievalService {

    private final PromptToSearchTextService promptToSearchTextService;
    private final EmbeddingClient embeddingClient;
    private final TrackEmbeddingRepository trackEmbeddingRepository;
    private final MmrReranker mmrReranker;
    private final NamedParameterJdbcTemplate primaryNamedJdbcTemplate;

    private static final int CANDIDATE_K = 30;       // ANN으로 가져올 후보
    private static final int FINAL_K = 8;            // LLM에 넘길 트랙 수
    private static final double LAMBDA = 0.6;        // MMR 다양성

    public List<RetrievedTrack> retrieve(String userMessage) {
        // 1) 검색 텍스트 변환
        String searchText = promptToSearchTextService.convert(userMessage);
        log.debug("검색 실행 텍스트: '{}'", searchText);

        // 2) 임베딩 + 벡터 검색
        float[] queryVector = embeddingClient.embed(searchText);
        List<Long> candidateIds = trackEmbeddingRepository.findSimilarTrackIds(
                queryVector, CANDIDATE_K, List.of());

        if (candidateIds.isEmpty()) return List.of();

        // 3) MMR 다양성
        Map<Long, float[]> vectors = trackEmbeddingRepository.findVectorsByTrackIds(candidateIds);
        List<Candidate> mmrInput = candidateIds.stream()
                .filter(vectors::containsKey)
                .map(id -> new Candidate(id, vectors.get(id)))
                .toList();

        List<Long> finalIds = mmrReranker.rerank(queryVector, mmrInput, FINAL_K, LAMBDA);

        // 4) 메타데이터
        return loadMetadata(finalIds);
    }

    private List<RetrievedTrack> loadMetadata(List<Long> ids) {
        if (ids.isEmpty()) return List.of();

        String sql = """
            SELECT
                t.id,
                t.title,
                ar.name AS artist,
                al.title AS album,
                t.genre,
                YEAR(t.published_at) AS release_year,
                al.cover_image_url AS cover
            FROM tracks t
            LEFT JOIN artists ar ON ar.id = t.artist_id
            LEFT JOIN albums  al ON al.id = t.album_id
            WHERE t.id IN (:ids)
            """;

        MapSqlParameterSource params = new MapSqlParameterSource("ids", ids);
        Map<Long, RetrievedTrack> map = new java.util.HashMap<>();
        primaryNamedJdbcTemplate.query(sql, params, rs -> {
            map.put(rs.getLong("id"), new RetrievedTrack(
                    rs.getLong("id"),
                    rs.getString("title"),
                    rs.getString("artist"),
                    rs.getString("album"),
                    rs.getString("genre"),
                    rs.getInt("release_year"),
                    rs.getString("cover")
            ));
        });

        // 검색 순서 유지
        return ids.stream().map(map::get).filter(java.util.Objects::nonNull).toList();
    }
}
