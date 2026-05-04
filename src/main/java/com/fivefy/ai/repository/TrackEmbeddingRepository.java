package com.fivefy.ai.repository;

import com.fivefy.ai.domain.TrackEmbedding;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * pgvector 테이블 직접 조작.
 *
 * 핵심:
 *  1) PGvector 클래스로 float[] ↔ pgvector 타입 변환
 *  2) UPSERT (INSERT ... ON CONFLICT) 로 멱등성 확보
 *  3) <=> 연산자가 코사인 거리 (1 - 코사인 유사도)
 *  4) 모든 동적 IN절은 NamedParameterJdbcTemplate 으로 바인딩 (SQL Injection 방어)
 *
 * 인덱스 전제:
 *   CREATE INDEX track_embedding_hnsw_idx
 *     ON track_embedding USING hnsw (embedding vector_cosine_ops);
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TrackEmbeddingRepository {

    private final JdbcTemplate vectorJdbcTemplate;
    private final NamedParameterJdbcTemplate vectorNamedJdbcTemplate;

    /**
     * UPSERT — 같은 trackId가 있으면 갱신.
     * source_hash가 같으면 굳이 update 안 함 (임베딩 호출 안 했을 거라 도달 X지만 안전장치).
     */
    public void upsert(TrackEmbedding e) {
        String sql = """
            INSERT INTO track_embedding
                (track_id, embedding, source_text, source_hash, embedded_at, model_version)
            VALUES
                (?, ?::vector, ?, ?, ?, ?)
            ON CONFLICT (track_id) DO UPDATE SET
                embedding = EXCLUDED.embedding,
                source_text = EXCLUDED.source_text,
                source_hash = EXCLUDED.source_hash,
                embedded_at = EXCLUDED.embedded_at,
                model_version = EXCLUDED.model_version
            WHERE track_embedding.source_hash <> EXCLUDED.source_hash
            """;

        vectorJdbcTemplate.update(
                sql,
                e.getTrackId(),
                new PGvector(e.getEmbedding()),
                e.getSourceText(),
                e.getSourceHash(),
                e.getEmbeddedAt(),
                e.getModelVersion()
        );
    }

    /**
     * 여러 트랙을 한 번에 UPSERT (배치 잡용).
     */
    public void upsertBatch(List<TrackEmbedding> embeddings) {
        if (embeddings.isEmpty()) return;

        String sql = """
            INSERT INTO track_embedding
                (track_id, embedding, source_text, source_hash, embedded_at, model_version)
            VALUES (?, ?::vector, ?, ?, ?, ?)
            ON CONFLICT (track_id) DO UPDATE SET
                embedding = EXCLUDED.embedding,
                source_text = EXCLUDED.source_text,
                source_hash = EXCLUDED.source_hash,
                embedded_at = EXCLUDED.embedded_at,
                model_version = EXCLUDED.model_version
            """;

        vectorJdbcTemplate.batchUpdate(sql, embeddings, embeddings.size(),
                (ps, e) -> {
                    ps.setLong(1, e.getTrackId());
                    ps.setObject(2, new PGvector(e.getEmbedding()));
                    ps.setString(3, e.getSourceText());
                    ps.setString(4, e.getSourceHash());
                    ps.setObject(5, e.getEmbeddedAt());
                    ps.setString(6, e.getModelVersion());
                });

        log.info("Upserted {} track embeddings", embeddings.size());
    }

    /**
     * 코사인 유사도 기반 Top-K 검색.
     * <=> 는 코사인 거리(0 = 동일, 2 = 정반대)이므로 ORDER BY ASC.
     *
     * NamedParameterJdbcTemplate 사용 — IN절 동적 길이를 안전하게 바인딩.
     */
    public List<Long> findSimilarTrackIds(float[] queryVector, int limit, List<Long> excludeIds) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("queryVec", new PGvector(queryVector))
                .addValue("limit", limit);

        String sql;
        if (excludeIds == null || excludeIds.isEmpty()) {
            sql = """
                SELECT track_id
                FROM track_embedding
                ORDER BY embedding <=> (:queryVec)::vector
                LIMIT :limit
                """;
        } else {
            params.addValue("excludeIds", excludeIds);
            sql = """
                SELECT track_id
                FROM track_embedding
                WHERE track_id NOT IN (:excludeIds)
                ORDER BY embedding <=> (:queryVec)::vector
                LIMIT :limit
                """;
        }

        return vectorNamedJdbcTemplate.query(sql, params,
                (rs, rn) -> rs.getLong("track_id"));
    }

    /**
     * 이미 임베딩된 트랙 ID 조회.
     */
    public List<Long> findAllTrackIds() {
        return vectorJdbcTemplate.queryForList(
                "SELECT track_id FROM track_embedding", Long.class);
    }

    /**
     * 여러 트랙 ID로 임베딩 일괄 조회 (유저 벡터 빌딩용).
     *
     * 람다는 RowCallbackHandler 로 매칭되어 매 row 호출됨.
     * (ResultSetExtractor 로 캐스팅하면 첫 row만 처리되는 사고 발생 — findHashesByTrackIds 참고)
     */
    public Map<Long, float[]> findVectorsByTrackIds(List<Long> trackIds) {
        if (trackIds == null || trackIds.isEmpty()) return Map.of();

        String sql = "SELECT track_id, embedding FROM track_embedding WHERE track_id IN (:ids)";
        MapSqlParameterSource params = new MapSqlParameterSource("ids", trackIds);

        Map<Long, float[]> result = new HashMap<>();
        vectorNamedJdbcTemplate.query(sql, params, rs -> {
            PGvector v = (PGvector) rs.getObject("embedding");
            result.put(rs.getLong("track_id"), v.toArray());
        });
        return result;
    }

    /**
     * 단일 트랙의 hash 조회.
     */
    public String findHashByTrackId(Long trackId) {
        try {
            return vectorJdbcTemplate.queryForObject(
                    "SELECT source_hash FROM track_embedding WHERE track_id = ?",
                    String.class, trackId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * 여러 트랙 ID의 hash를 한 번에 조회 (배치 잡 N+1 방지).
     * 결과에 없는 trackId는 임베딩이 아직 없는 것 → 호출자가 신규 처리.
     *
     * 구현 주의:
     *  ResultSetExtractor 로 명시 — 람다 추론이 RowCallbackHandler/ResultSetExtractor 사이에서
     *  애매하게 매칭되어 "첫 row만 처리되고 종료"되는 사고를 막기 위해 while(rs.next()) 로 직접 순회.
     */
    public Map<Long, String> findHashesByTrackIds(List<Long> trackIds) {
        if (trackIds == null || trackIds.isEmpty()) return Map.of();

        String sql = "SELECT track_id, source_hash FROM track_embedding WHERE track_id IN (:ids)";
        MapSqlParameterSource params = new MapSqlParameterSource("ids", trackIds);

        return vectorNamedJdbcTemplate.query(sql, params, rs -> {
            Map<Long, String> map = new HashMap<>();
            while (rs.next()) {
                map.put(rs.getLong("track_id"), rs.getString("source_hash"));
            }
            return map;
        });
    }
}
