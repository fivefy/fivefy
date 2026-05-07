package com.fivefy.ai.job;

import com.fivefy.ai.dto.etc.TrackForEmbedding;
import com.fivefy.ai.service.TrackEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrackEmbeddingJob {

    private final TrackEmbeddingService embeddingService;
    private final JdbcTemplate primaryJdbcTemplate;

    private static final int MAX_ITERATIONS = 10_000;
    private static final int batchSize = 100;

    // 매일 새벽 3시 실행
    @Scheduled(cron = "0 0 3 * * *")
    public void runDailyEmbedding() {
        log.info("=== 일일 트랙 임베딩 작업 시작 ===");
        long startMs = System.currentTimeMillis();

        int totalProcessed = 0;
        int totalSkipped = 0;
        int totalFailed = 0;
        long lastTrackId = 0;
        int iteration = 0;

        while (iteration++ < MAX_ITERATIONS) {
            List<TrackForEmbedding> chunk = fetchTrackChunk(lastTrackId);
            if (chunk.isEmpty()) break;

            long beforeChunkLastId = lastTrackId;
            try {
                var result = embeddingService.embedBatch(chunk);
                totalProcessed += result.processed();
                totalSkipped += result.skipped();
                totalFailed += result.failed();
            } catch (Exception e) {
                log.error("청크 임베딩 실패 (lastTrackId={}), 다음 청크 진행...", lastTrackId, e);
                totalFailed += chunk.size();
            }

            lastTrackId = chunk.get(chunk.size() - 1).trackId();

            // 안전장치: lastTrackId가 전혀 진척 없으면 동일 청크 무한반복 위험 → 중단
            if (lastTrackId <= beforeChunkLastId) {
                log.error("작업 ID가 갱신되지 않았습니다 ({} -> {}), 무한 루프 방지를 위해 작업을 중단합니다",
                        beforeChunkLastId, lastTrackId);
                break;
            }
        }

        if (iteration >= MAX_ITERATIONS) {
            log.error("최대 반복 횟수 ({}) 도달: 작업 강제 종료; 일부 트랙의 처리가 누락되었을 수 있습니다",
                    MAX_ITERATIONS);
        }

        long elapsedSec = (System.currentTimeMillis() - startMs) / 1000;
        log.info("=== 일일 트랙 임베딩 작업 완료 in {}s: processed={}, skipped={}, failed={}, iterations={} ===",
                elapsedSec, totalProcessed, totalSkipped, totalFailed, iteration);
    }

    private List<TrackForEmbedding> fetchTrackChunk(long lastTrackId) {
        String sql = """
            SELECT
                t.id              AS track_id,
                t.title           AS title,
                ar.name           AS artist,
                al.title          AS album,
                t.genre           AS genre,
                YEAR(t.published_at) AS release_year,
                t.featured_artist_text AS featured
            FROM tracks t
            LEFT JOIN artists ar ON ar.id = t.artist_id
            LEFT JOIN albums  al ON al.id = t.album_id
            WHERE t.id > ?
              AND t.deleted_at IS NULL
              AND t.status = 'PUBLISHED'
            ORDER BY t.id
            LIMIT ?
            """;

        return primaryJdbcTemplate.query(sql,
                (rs, rn) -> new TrackForEmbedding(
                        rs.getLong("track_id"),
                        rs.getString("title"),
                        rs.getString("artist"),
                        rs.getString("album"),
                        rs.getString("genre"),
                        rs.getInt("release_year"),
                        rs.getString("featured")
                ),
                lastTrackId, TrackEmbeddingJob.batchSize);
    }
}
