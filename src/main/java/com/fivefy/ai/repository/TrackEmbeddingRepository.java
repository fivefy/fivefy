package com.fivefy.ai.repository;

import com.fivefy.ai.domain.TrackEmbedding;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TrackEmbeddingRepository {

    @Qualifier("vectorJdbcTemplate")
    private final JdbcTemplate vectorJdbcTemplate;
    @Qualifier("vectorNamedJdbcTemplate")
    private final NamedParameterJdbcTemplate vectorNamedJdbcTemplate;

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

    public List<Long> findAllTrackIds() {
        return vectorJdbcTemplate.queryForList(
                "SELECT track_id FROM track_embedding", Long.class);
    }

    public Map<Long, float[]> findVectorsByTrackIds(List<Long> trackIds) {
        if (trackIds == null || trackIds.isEmpty()) return Map.of();

        String sql = "SELECT track_id, embedding FROM track_embedding WHERE track_id IN (:ids)";
        MapSqlParameterSource params = new MapSqlParameterSource("ids", trackIds);

        Map<Long, float[]> result = new HashMap<>();
        vectorNamedJdbcTemplate.query(sql, params, rs -> {
            org.postgresql.util.PGobject pgObj = (org.postgresql.util.PGobject) rs.getObject("embedding");
            if (pgObj != null) {
                com.pgvector.PGvector v = new com.pgvector.PGvector(pgObj.getValue());
                result.put(rs.getLong("track_id"), v.toArray());
            }
        });
        return result;
    }

    public String findHashByTrackId(Long trackId) {
        try {
            return vectorJdbcTemplate.queryForObject(
                    "SELECT source_hash FROM track_embedding WHERE track_id = ?",
                    String.class, trackId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

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
