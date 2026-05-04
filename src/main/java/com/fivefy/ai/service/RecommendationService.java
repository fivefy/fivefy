package com.fivefy.ai.service;

import com.fivefy.ai.domain.UserEmbedding;
import com.fivefy.ai.dto.RecommendationResponse;
import com.fivefy.ai.observability.AiBusinessMetrics;
import com.fivefy.ai.repository.TrackEmbeddingRepository;
import com.fivefy.ai.repository.UserEmbeddingRepository;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.fivefy.ai.observability.AiBusinessMetrics.Feature.RECOMMENDATION;

/**
 * 개인화 추천 메인 서비스.
 *
 * 4단계 흐름:
 *  1) 유저 벡터 조회 (캐시 → DB → 신규 계산)
 *  2) pgvector ANN 검색으로 후보 K개
 *  3) MMR 다양성 재정렬 → 최종 N개
 *  4) MySQL에서 메타데이터 join
 *
 * 캐시 TTL: 6시간 (재생 패턴이 여섯 시간 안에 크게 안 바뀜)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserEmbeddingRepository userEmbeddingRepository;
    private final TrackEmbeddingRepository trackEmbeddingRepository;
    private final UserVectorBuilder userVectorBuilder;
    private final MmrReranker mmrReranker;
    private final JdbcTemplate primaryJdbcTemplate;                       // 단순 위치 바인딩
    private final NamedParameterJdbcTemplate primaryNamedJdbcTemplate;    // IN 절 안전 바인딩
    private final AiBusinessMetrics metrics;

    // K 배수: 후보를 N개의 몇 배 가져올지 (MMR 여유)
    private static final int CANDIDATE_MULTIPLIER = 3;

    // 캐시된 유저 벡터가 이 시간보다 오래되면 재계산
    private static final Duration STALE_AFTER = Duration.ofHours(6);

    public RecommendationResponse recommendForUser(Long userId, int limit) {
        Timer.Sample totalTimer = metrics.startTimer();

        try {
            // ─── 1. 유저 벡터 확보 ───
            Timer.Sample retrievalTimer = metrics.startTimer();
            float[] userVector = resolveUserVector(userId);
            if (userVector == null) {
                metrics.recordColdStart(RECOMMENDATION);
                log.info("Cold start for userId={}, returning popular tracks", userId);
                RecommendationResponse fallback = coldStartFallback(userId, limit);
                metrics.recordResultCount(RECOMMENDATION, fallback.tracks().size());
                metrics.recordCall(RECOMMENDATION, true);
                return fallback;
            }

            // ─── 2. 후보 검색 ───
            int k = limit * CANDIDATE_MULTIPLIER;
            List<Long> excludeIds = fetchRecentlyPlayedIds(userId, 100);
            List<Long> candidateIds = trackEmbeddingRepository.findSimilarTrackIds(
                    userVector, k, excludeIds);
            metrics.recordLatency(retrievalTimer, RECOMMENDATION, "retrieval");

            if (candidateIds.isEmpty()) {
                metrics.recordEmptyResult(RECOMMENDATION);
                metrics.recordCall(RECOMMENDATION, true);
                return coldStartFallback(userId, limit);
            }

            // ─── 3. MMR 재정렬 ───
            Timer.Sample rerankTimer = metrics.startTimer();
            Map<Long, float[]> candidateVectors = trackEmbeddingRepository
                    .findVectorsByTrackIds(candidateIds);

            List<MmrReranker.Candidate> mmrInput = candidateIds.stream()
                    .filter(candidateVectors::containsKey)
                    .map(id -> new MmrReranker.Candidate(id, candidateVectors.get(id)))
                    .toList();

            List<Long> finalIds = mmrReranker.rerank(userVector, mmrInput, limit);
            metrics.recordLatency(rerankTimer, RECOMMENDATION, "rerank");

            // ─── 4. 메타데이터 join ───
            List<RecommendationResponse.RecommendedTrack> tracks =
                    enrichWithMetadata(finalIds, userVector, candidateVectors);

            metrics.recordResultCount(RECOMMENDATION, tracks.size());
            metrics.recordCall(RECOMMENDATION, true);

            return new RecommendationResponse(tracks, null, getActionCount(userId));
        } catch (Exception e) {
            // 메트릭 기록이 실패해도 원본 예외를 잃지 않게 보호
            try { metrics.recordCall(RECOMMENDATION, false); } catch (Exception ignored) {}
            throw e;
        } finally {
            try { metrics.recordLatency(totalTimer, RECOMMENDATION, "total"); } catch (Exception ignored) {}
        }
    }

    /**
     * 유저 벡터: 캐시 → DB → 신규 계산.
     */
    private float[] resolveUserVector(Long userId) {
        var existingOpt = userEmbeddingRepository.findByUserId(userId);

        if (existingOpt.isPresent()) {
            UserEmbedding ue = existingOpt.get();
            // 너무 오래되지 않았으면 그대로 사용
            if (Duration.between(ue.getComputedAt(), LocalDateTime.now()).compareTo(STALE_AFTER) < 0) {
                return ue.getEmbedding();
            }
            log.debug("User vector stale for userId={}, rebuilding", userId);
        }

        // 신규 계산
        var result = userVectorBuilder.build(userId);
        if (!result.success()) return null;

        // DB에 저장 (다음에는 바로 hit)
        userEmbeddingRepository.upsert(UserEmbedding.builder()
                .userId(userId)
                .embedding(result.vector())
                .basedOnCount(result.sourceCount())
                .computedAt(LocalDateTime.now())
                .build());

        return result.vector();
    }

    /**
     * Cold start — 임베딩 만들 만큼의 재생 기록이 없는 경우.
     * 기존 popular_chart 도메인 재사용.
     */
    private RecommendationResponse coldStartFallback(Long userId, int limit) {
        String sql = """
            SELECT
                t.id,
                t.title,
                ar.name AS artist,
                al.cover_image_url AS cover
            FROM popular_charts pc
            JOIN tracks t ON t.id = pc.track_id
            LEFT JOIN artists ar ON ar.id = t.artist_id
            LEFT JOIN albums  al ON al.id = t.album_id
            WHERE pc.snapshot_date = (SELECT MAX(snapshot_date) FROM popular_charts)
              AND t.deleted_at IS NULL
              AND t.status = 'PUBLISHED'
            ORDER BY pc.chart_rank ASC
            LIMIT ?
            """;

        List<RecommendationResponse.RecommendedTrack> tracks = primaryJdbcTemplate.query(sql,
                (rs, rn) -> new RecommendationResponse.RecommendedTrack(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("artist"),
                        rs.getString("cover"),
                        0.5f
                ),
                limit);

        return new RecommendationResponse(tracks, "인기 트랙 기반 (이용 기록 부족)", 0);
    }

    /**
     * 최근 재생한 트랙 ID 조회 (추천에서 제외용).
     */
    private List<Long> fetchRecentlyPlayedIds(Long userId, int limit) {
        return primaryJdbcTemplate.queryForList("""
                SELECT track_id
                FROM (
                    SELECT track_id, last_played_at AS at_ts
                    FROM playbacks
                    WHERE user_id = ?
                      AND last_played_at > NOW() - INTERVAL 7 DAY
                    UNION ALL
                    SELECT target_id AS track_id, created_at AS at_ts
                    FROM likes
                    WHERE user_id = ?
                      AND target_type = 'TRACK'
                      AND created_at > NOW() - INTERVAL 7 DAY
                ) recent
                GROUP BY track_id
                ORDER BY MAX(at_ts) DESC
                LIMIT ?
                """, Long.class, userId, userId, limit);
    }

    /**
     * 90일 내 유저 액션 수 (basedOnCount, 추천 신뢰도 지표).
     */
    private int getActionCount(Long userId) {
        Integer count = primaryJdbcTemplate.queryForObject("""
                SELECT (
                    (SELECT COUNT(*) FROM playbacks
                     WHERE user_id = ? AND last_played_at > NOW() - INTERVAL 90 DAY)
                  + (SELECT COUNT(*) FROM likes
                     WHERE user_id = ? AND target_type = 'TRACK'
                       AND created_at > NOW() - INTERVAL 90 DAY)
                ) AS total
                """, Integer.class, userId, userId);
        return count == null ? 0 : count;
    }

    /**
     * 트랙 ID 리스트 → 메타데이터 + 점수.
     * finalIds 순서를 그대로 유지 (MMR 결과 순서 = 추천 순서).
     *
     * IN 절은 NamedParameterJdbcTemplate 으로 안전 바인딩.
     * (String concat 패턴은 SQL Injection 위험을 코드베이스에 퍼뜨림)
     */
    private List<RecommendationResponse.RecommendedTrack> enrichWithMetadata(
            List<Long> finalIds,
            float[] userVector,
            Map<Long, float[]> vectors) {

        if (finalIds.isEmpty()) return List.of();

        String sql = """
        SELECT t.id, t.title, ar.name AS artist, al.cover_image_url AS cover
        FROM tracks t
        LEFT JOIN artists ar ON ar.id = t.artist_id
        LEFT JOIN albums  al ON al.id = t.album_id
        WHERE t.id IN (:ids)
        """;
        MapSqlParameterSource params = new MapSqlParameterSource("ids", finalIds);

        Map<Long, RawTrack> meta = new HashMap<>();
        primaryNamedJdbcTemplate.query(sql, params, rs -> {
            meta.put(rs.getLong("id"),
                    new RawTrack(
                            rs.getLong("id"),
                            rs.getString("title"),
                            rs.getString("artist"),
                            rs.getString("cover")));
        });

        // finalIds 순서대로 결과 구성
        List<RecommendationResponse.RecommendedTrack> result = new ArrayList<>(finalIds.size());
        for (Long id : finalIds) {
            RawTrack m = meta.get(id);
            if (m == null) continue;
            float[] v = vectors.get(id);
            float score = (v == null) ? 0f : cosineSim(userVector, v);
            result.add(new RecommendationResponse.RecommendedTrack(
                    m.id, m.title, m.artist, m.cover, score));
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

    private record RawTrack(Long id, String title, String artist, String cover) {}
}
