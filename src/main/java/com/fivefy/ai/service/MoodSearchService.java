package com.fivefy.ai.service;

import com.fivefy.ai.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 무드 검색 메인 서비스.
 *
 * 흐름:
 *   1) 자연어 → LLM이 검색 텍스트 변환 (3단계 재사용)
 *   2) 검색 텍스트 임베딩
 *   3) 하이브리드 검색 (메타 + 가사)
 *   4) 메타데이터 join → 응답
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MoodSearchService {

    private final PromptToSearchTextService promptConverter;
    private final EmbeddingClient embeddingClient;
    private final HybridSemanticSearchService hybridSearch;
    private final JdbcTemplate primaryJdbcTemplate;

    public MoodSearchResponse search(MoodSearchRequest request) {
        // 1) 자연어 → 검색 텍스트
        String searchText = promptConverter.convert(request.query());
        log.debug("Mood search text: '{}' → '{}'", request.query(), searchText);

        // 2) 임베딩
        float[] queryVector = embeddingClient.embed(searchText);

        // 3) 하이브리드 검색
        List<ScoredResult> results = hybridSearch.search(queryVector, request.limit(), request.alpha());
        if (results.isEmpty()) {
            return new MoodSearchResponse(searchText, request.mode().name(), List.of());
        }

        // 4) 메타데이터 join
        List<Long> ids = results.stream().map(ScoredResult::trackId).toList();
        Map<Long, RawTrack> meta = loadTrackMeta(ids);

        List<MoodTrack> tracks = results.stream()
                .map(r -> {
                    RawTrack m = meta.get(r.trackId());
                    if (m == null) return null;
                    return new MoodTrack(
                            m.id(), m.title(), m.artist(), m.cover(),
                            r.finalScore(), r.metaScore(), r.lyricsScore(),
                            r.hasLyricsEmbedding());
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        return new MoodSearchResponse(searchText, request.mode().name(), tracks);
    }

    private Map<Long, RawTrack> loadTrackMeta(List<Long> ids) {
        if (ids.isEmpty()) return Map.of();

        String inClause = ids.stream().map(String::valueOf)
                .reduce((a, b) -> a + "," + b).orElse("");
        String sql = """
                SELECT
                    t.id,
                    t.title,
                    ar.name AS artist,
                    al.cover_image_url AS cover
                FROM tracks t
                LEFT JOIN artists ar ON ar.id = t.artist_id
                LEFT JOIN albums  al ON al.id = t.album_id
                WHERE t.id IN (%s)
                """.formatted(inClause);

        Map<Long, RawTrack> result = new java.util.HashMap<>();
        primaryJdbcTemplate.query(sql, (RowCallbackHandler) rs ->
                result.put(rs.getLong("id"),
                        new RawTrack(
                                rs.getLong("id"),
                                rs.getString("title"),
                                rs.getString("artist"),
                                rs.getString("cover"))));
        return result;
    }
}
