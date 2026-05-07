package com.fivefy.ai.job;

import com.fivefy.ai.dto.etc.TrackLyricsForEmbedding;
import com.fivefy.ai.service.LyricsEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LyricsEmbeddingJob {

    private static final int MAX_ITERATIONS = 10_000;

    private final LyricsEmbeddingService lyricsEmbeddingService;
    private final JdbcTemplate primaryJdbcTemplate;

    private static final int batchSize = 50;

    // 매일 새벽 4시 (트랙 임베딩 잡 끝난 후)
    @Scheduled(cron = "0 0 4 * * *")
    public void runDailyEmbedding() {
        log.info("=== 일일 가사 임베딩 작업 시작 ===");
        long startMs = System.currentTimeMillis();

        int processed = 0, skipped = 0, failed = 0;
        long lastTrackId = 0;
        int iteration = 0;

        while (iteration++ < MAX_ITERATIONS) {
            List<TrackLyricsForEmbedding> chunk = fetchLyricsChunk(lastTrackId);
            if (chunk.isEmpty()) break;

            for (TrackLyricsForEmbedding lyrics : chunk) {
                try {
                    boolean done = lyricsEmbeddingService.embedLyrics(lyrics);
                    if (done) processed++; else skipped++;
                } catch (Exception e) {
                    log.error("가사 임베딩 작업 실패: trackId={}", lyrics.trackId(), e);
                    failed++;
                }

                // Rate limit 보호
                sleep(150);
            }

            lastTrackId = chunk.get(chunk.size() - 1).trackId();
        }

        if (iteration >= MAX_ITERATIONS) {
            log.error("최대 반복 횟수 ({}) 도달: 작업 강제 종료; 일부 트랙의 처리가 누락되었을 수 있습니다",
                    MAX_ITERATIONS);
        }

        long elapsedSec = (System.currentTimeMillis() - startMs) / 1000;
        log.info("=== 일일 가사 임베딩 작업 완료 in {}s: processed={}, skipped={}, failed={}, iterations={} ===",
                elapsedSec, processed, skipped, failed, iteration);
    }

    // tracks.lyrics 컬럼에서 가사 직접 조회
    private List<TrackLyricsForEmbedding> fetchLyricsChunk(long lastTrackId) {
        String sql = """
            SELECT id AS track_id, lyrics
            FROM tracks
            WHERE id > ?
              AND lyrics IS NOT NULL
              AND CHAR_LENGTH(lyrics) >= 50
              AND deleted_at IS NULL
              AND status = 'PUBLISHED'
            ORDER BY id
            LIMIT ?
            """;

        return primaryJdbcTemplate.query(sql,
                (rs, rn) -> new TrackLyricsForEmbedding(
                        rs.getLong("track_id"),
                        rs.getString("lyrics")
                ),
                lastTrackId, LyricsEmbeddingJob.batchSize);
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
