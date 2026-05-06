package com.fivefy.ai.repository;

import com.fivefy.ai.domain.TrackLyricsEmbedding;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TrackLyricsEmbeddingRepository {

    @Qualifier("vectorJdbcTemplate")
    private final JdbcTemplate vectorJdbcTemplate;
    @Qualifier("vectorNamedJdbcTemplate")
    private final NamedParameterJdbcTemplate vectorNamedJdbcTemplate;

    public void upsert(TrackLyricsEmbedding e) {
        String sql = """
            INSERT INTO track_lyrics_embedding
                (track_id, embedding, snippet, chunk_count, source_hash, embedded_at, model_version)
            VALUES (?, ?::vector, ?, ?, ?, ?, ?)
            ON CONFLICT (track_id) DO UPDATE SET
                embedding = EXCLUDED.embedding,
                snippet = EXCLUDED.snippet,
                chunk_count = EXCLUDED.chunk_count,
                source_hash = EXCLUDED.source_hash,
                embedded_at = EXCLUDED.embedded_at,
                model_version = EXCLUDED.model_version
            WHERE track_lyrics_embedding.source_hash <> EXCLUDED.source_hash
            """;

        vectorJdbcTemplate.update(
                sql,
                e.getTrackId(),
                new PGvector(e.getEmbedding()),
                e.getSnippet(),
                e.getChunkCount(),
                e.getSourceHash(),
                e.getEmbeddedAt(),
                e.getModelVersion()
        );
    }

    public Map<Long, Float> getSimilarityScoresFor(float[] queryVector, List<Long> trackIds) {
        if (trackIds.isEmpty()) return Map.of();

        String sql = """
        SELECT track_id,
               1 - (embedding <=> :queryVector::vector) AS similarity
        FROM track_lyrics_embedding
        WHERE track_id IN (:ids)
        ORDER BY similarity DESC
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("queryVector", new PGvector(queryVector))
                .addValue("ids", trackIds);
        Map<Long, Float> result = new java.util.LinkedHashMap<>();
        vectorNamedJdbcTemplate.query(sql, params, rs -> {
            result.put(rs.getLong("track_id"), rs.getFloat("similarity"));
        });
        return result;
    }

    public String findHashByTrackId(Long trackId) {
        try {
            return vectorJdbcTemplate.queryForObject(
                    "SELECT source_hash FROM track_lyrics_embedding WHERE track_id = ?",
                    String.class, trackId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}
