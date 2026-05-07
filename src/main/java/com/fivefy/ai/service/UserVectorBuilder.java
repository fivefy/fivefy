package com.fivefy.ai.service;

import com.fivefy.ai.dto.etc.BuildResult;
import com.fivefy.ai.repository.TrackEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserVectorBuilder {

    private final JdbcTemplate primaryJdbcTemplate;
    private final TrackEmbeddingRepository trackEmbeddingRepository;

    private static final int MAX_SOURCE_TRACKS = 50;  // 너무 많으면 평균이 평탄해짐
    private static final double MIN_TOTAL_WEIGHT = 1e-6;

    // 가중치 상수 (튜닝 대상)
    private static final double HALF_LIFE_DAYS = 30.0;     // 30일 지나면 가중치 절반
    private static final double WEIGHT_LIKE = 3.0;         // 좋아요
    private static final double WEIGHT_FULL_PLAY = 1.0;    // 완청
    private static final double WEIGHT_PARTIAL_PLAY = 0.5; // 부분 재생
    // SKIP 은 0 (무시). 향후 negative signal 이 필요하면 별도 채널로.

    public BuildResult build(Long userId) {
        List<UserAction> actions = fetchRecentActions(userId, MAX_SOURCE_TRACKS);
        if (actions.isEmpty()) {
            log.debug("사용자 활동 기록 없음 for userId={}, 벡터 생성 불가", userId);
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

    private List<UserAction> fetchRecentActions(Long userId, int limit) {
        String sql = """
            (
                -- 좋아요는 강한 시그널, play_count=1로 통일
                SELECT
                    target_id  AS track_id,
                    'LIKE'     AS action_type,
                    created_at AS action_at,
                    1          AS play_count
                FROM likes
                WHERE user_id = ?
                  AND target_type = 'TRACK'
                  AND created_at > NOW() - INTERVAL 90 DAY
            )
            UNION ALL
            (
                -- 재생은 같은 트랙이 여러 번이라 GROUP BY로 집계
                SELECT
                    p.track_id,
                    CASE
                        WHEN p.played_duration >= 90 THEN 'FULL_PLAY'
                        WHEN p.played_duration >= 30 THEN 'PARTIAL_PLAY'
                        ELSE 'SKIP'
                    END                       AS action_type,
                    MAX(p.last_played_at)     AS action_at,
                    COUNT(*)                  AS play_count
                FROM playbacks p
                WHERE p.user_id = ?
                  AND p.last_played_at > NOW() - INTERVAL 90 DAY
                GROUP BY p.track_id,
                    CASE
                        WHEN p.played_duration >= 90 THEN 'FULL_PLAY'
                        WHEN p.played_duration >= 30 THEN 'PARTIAL_PLAY'
                        ELSE 'SKIP'
                    END
            )
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
                userId, userId, limit);

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
            LocalDateTime actionAt, int playCount
    ) {}
}
