package com.fivefy.ai.service;

import com.fivefy.ai.repository.TrackEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 유저 벡터 빌더 — 재생 기록 → 유저 취향 벡터.
 *
 * 가중 평균 공식:
 *   user_vec = Σ (track_vec_i × weight_i) / Σ weight_i
 *
 *   weight_i = recency_weight × action_weight × play_count_weight
 *
 * - recency_weight: 최근에 들을수록 큰 가중치 (지수 감쇠)
 * - action_weight : 좋아요/완청/부분재생에 따라 다른 가중치
 * - play_count    : 자주 들은 곡일수록 큰 가중치 (log scale 로 완화)
 *
 * 스킵(SKIP) 처리:
 *  스킵을 음수 가중치로 평균에 섞으면 분모가 0 근방으로 가서 결과가 폭주할 수 있고,
 *  표준 가중 평균 의미가 깨짐. 1차 구현에서는 SKIP 액션을 가중치 0 (무시) 으로 처리.
 *  나중에 negative signal 을 쓰려면 별도 anti-vector 를 만들어 검색 단계에서 페널티로
 *  주는 방식이 안전 (이 클래스 책임 밖).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserVectorBuilder {

    private final JdbcTemplate primaryJdbcTemplate;  // MySQL
    private final TrackEmbeddingRepository trackEmbeddingRepository;

    private static final int MAX_SOURCE_TRACKS = 50;  // 너무 많으면 평균이 평탄해짐
    private static final double MIN_TOTAL_WEIGHT = 1e-6;

    // 가중치 상수 (튜닝 대상)
    private static final double HALF_LIFE_DAYS = 30.0;     // 30일 지나면 가중치 절반
    private static final double WEIGHT_LIKE = 3.0;         // 좋아요
    private static final double WEIGHT_FULL_PLAY = 1.0;    // 완청 (90% 이상)
    private static final double WEIGHT_PARTIAL_PLAY = 0.5; // 부분 재생
    // SKIP 은 0 (무시). 향후 negative signal 이 필요하면 별도 채널로.

    /**
     * 유저 벡터 생성. 재생 기록이 부족하면 Optional.empty.
     */
    public BuildResult build(Long userId) {
        List<UserAction> actions = fetchRecentActions(userId, MAX_SOURCE_TRACKS);
        if (actions.isEmpty()) {
            log.debug("No actions for userId={}, cannot build vector", userId);
            return BuildResult.empty();
        }

        // 첫 액션 벡터 길이로 차원 결정 (모델 교체 시 하드코딩 누락 방지)
        int dim = actions.get(0).vector.length;
        float[] accumulated = new float[dim];
        double totalWeight = 0.0;

        LocalDateTime now = LocalDateTime.now();
        for (UserAction action : actions) {
            double weight = computeWeight(action, now);
            if (weight <= 0.0) continue;  // 0 또는 음수는 스킵 (현재는 0만 발생)

            for (int i = 0; i < dim; i++) {
                accumulated[i] += (float) (weight * action.vector[i]);
            }
            totalWeight += weight;
        }

        if (totalWeight < MIN_TOTAL_WEIGHT) {
            return BuildResult.empty();
        }

        // 가중 평균
        for (int i = 0; i < dim; i++) {
            accumulated[i] /= (float) totalWeight;
        }

        // L2 정규화 (코사인 유사도용)
        normalize(accumulated);

        return BuildResult.of(accumulated, actions.size());
    }

    /**
     * 단일 액션의 가중치 계산. 음수 가중치는 발생하지 않음.
     */
    private double computeWeight(UserAction action, LocalDateTime now) {
        long daysAgo = Duration.between(action.actionAt, now).toDays();
        double recencyWeight = Math.pow(0.5, daysAgo / HALF_LIFE_DAYS);

        double actionWeight = switch (action.actionType) {
            case "LIKE"          -> WEIGHT_LIKE;
            case "FULL_PLAY"     -> WEIGHT_FULL_PLAY;
            case "PARTIAL_PLAY"  -> WEIGHT_PARTIAL_PLAY;
            case "SKIP"          -> 0.0;
            default              -> 0.0;
        };

        // 100번 들었다고 100배 가중되면 한 곡에 매몰됨 → log scale
        double playCountWeight = Math.log1p(action.playCount);

        return recencyWeight * actionWeight * playCountWeight;
    }

    /**
     * 두 단계 조회:
     *  1) MySQL 에서 액션 + track_id
     *  2) PostgreSQL 에서 track_id IN (...) 으로 임베딩 일괄 조회
     * in-memory 에서 합침.
     */
    private List<UserAction> fetchRecentActions(Long userId, int limit) {
        String sql = """
            SELECT track_id, action_type, action_at, play_count
            FROM user_action
            WHERE user_id = ?
              AND action_at > NOW() - INTERVAL 90 DAY
            ORDER BY action_at DESC
            LIMIT ?
            """;

        List<RawAction> rawActions = primaryJdbcTemplate.query(sql,
                (rs, rn) -> new RawAction(
                        rs.getLong("track_id"),
                        rs.getString("action_type"),
                        rs.getTimestamp("action_at").toLocalDateTime(),
                        rs.getInt("play_count")
                ),
                userId, limit);

        if (rawActions.isEmpty()) return List.of();

        List<Long> trackIds = rawActions.stream().map(RawAction::trackId).distinct().toList();
        Map<Long, float[]> vectors = trackEmbeddingRepository.findVectorsByTrackIds(trackIds);

        return rawActions.stream()
                .filter(a -> vectors.containsKey(a.trackId()))
                .map(a -> new UserAction(
                        a.trackId(),
                        vectors.get(a.trackId()),
                        a.actionType(),
                        a.actionAt(),
                        a.playCount()))
                .toList();
    }

    private void normalize(float[] v) {
        double sumSq = 0.0;
        for (float x : v) sumSq += x * x;
        double norm = Math.sqrt(sumSq);
        if (norm == 0.0) return;
        for (int i = 0; i < v.length; i++) v[i] /= (float) norm;
    }

    // ─── DTOs ───
    private record RawAction(Long trackId, String actionType, LocalDateTime actionAt, int playCount) {}

    private record UserAction(
            Long trackId, float[] vector, String actionType,
            LocalDateTime actionAt, int playCount) {}

    public record BuildResult(float[] vector, int sourceCount, boolean success) {
        static BuildResult empty() { return new BuildResult(null, 0, false); }
        static BuildResult of(float[] v, int n) { return new BuildResult(v, n, true); }
    }
}
