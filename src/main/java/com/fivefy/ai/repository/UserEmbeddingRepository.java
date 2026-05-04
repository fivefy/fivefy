package com.fivefy.ai.repository;

import com.fivefy.ai.domain.UserEmbedding;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class UserEmbeddingRepository {

    private final JdbcTemplate vectorJdbcTemplate;

    public Optional<UserEmbedding> findByUserId(Long userId) {
        String sql = """
            SELECT user_id, embedding, based_on_count, computed_at
            FROM user_embedding
            WHERE user_id = ?
            """;
        try {
            UserEmbedding ue = vectorJdbcTemplate.queryForObject(sql,
                    (rs, rn) -> {
                        PGvector v = (PGvector) rs.getObject("embedding");
                        return UserEmbedding.builder()
                                .userId(rs.getLong("user_id"))
                                .embedding(v.toArray())
                                .basedOnCount(rs.getInt("based_on_count"))
                                .computedAt(rs.getTimestamp("computed_at").toLocalDateTime())
                                .build();
                    },
                    userId);
            return Optional.ofNullable(ue);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void upsert(UserEmbedding ue) {
        String sql = """
            INSERT INTO user_embedding (user_id, embedding, based_on_count, computed_at)
            VALUES (?, ?::vector, ?, ?)
            ON CONFLICT (user_id) DO UPDATE SET
                embedding = EXCLUDED.embedding,
                based_on_count = EXCLUDED.based_on_count,
                computed_at = EXCLUDED.computed_at
            """;
        vectorJdbcTemplate.update(sql,
                ue.getUserId(),
                new PGvector(ue.getEmbedding()),
                ue.getBasedOnCount(),
                ue.getComputedAt());
    }
}
