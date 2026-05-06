package com.fivefy.ai.job;

import com.fivefy.ai.dto.TrackLyricsForEmbedding;
import com.fivefy.ai.service.LyricsEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 가사 임베딩 배치 잡.
 *
 * 트랙 임베딩 잡과 비슷하지만:
 *  - 가사 한 곡당 호출 수가 더 많음 (청크 수만큼)
 *  - 따라서 청크 단위가 아닌 *트랙 단위* 처리 (한 곡씩 순차)
 *  - rate limit 더 보수적으로 (한 번에 적게)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fivefy.ai.lyrics-embedding.enabled", havingValue = "true")
public class LyricsEmbeddingJob {

    private static final int MAX_ITERATIONS = 10_000;

    private final LyricsEmbeddingService lyricsEmbeddingService;
    private final JdbcTemplate primaryJdbcTemplate;

    @Value("${fivefy.ai.lyrics-embedding.batch-size:50}")
    private int batchSize;

    /**
     * 매일 새벽 4시 (트랙 임베딩 잡 끝난 후).
     */
    @Scheduled(cron = "${fivefy.ai.lyrics-embedding.cron:0 0 4 * * *}")
    public void runDailyEmbedding() {
        log.info("=== Daily lyrics embedding job started ===");
        long startMs = System.currentTimeMillis();

        int processed = 0, skipped = 0, failed = 0;
        long lastTrackId = 0;
        int iteration = 0;

        while (iteration++ < MAX_ITERATIONS) {
            List<TrackLyricsForEmbedding> chunk = fetchLyricsChunk(lastTrackId, batchSize);
            if (chunk.isEmpty()) break;

            for (TrackLyricsForEmbedding lyrics : chunk) {
                try {
                    boolean done = lyricsEmbeddingService.embedLyrics(lyrics);
                    if (done) processed++; else skipped++;
                } catch (Exception e) {
                    log.error("Lyrics embedding failed: trackId={}", lyrics.trackId(), e);
                    failed++;
                }

                // Rate limit 보호
                sleep(150);
            }

            lastTrackId = chunk.get(chunk.size() - 1).trackId();
        }

        if (iteration >= MAX_ITERATIONS) {
            log.error("Hit MAX_ITERATIONS ({}). Job aborted; some tracks may be unprocessed.",
                    MAX_ITERATIONS);
        }

        long elapsedSec = (System.currentTimeMillis() - startMs) / 1000;
        log.info("=== Lyrics embedding done in {}s: processed={}, skipped={}, failed={}, iterations={} ===",
                elapsedSec, processed, skipped, failed, iteration);
    }

    /**
     * tracks.lyrics 컬럼에서 가사 직접 조회.
     *
     * fivefy는 별도 lyrics 테이블 없이 tracks 테이블에 lyrics TEXT 컬럼 보유.
     * - lyrics IS NOT NULL & 50자 이상만 임베딩 (의미 있는 가사만)
     * - PUBLISHED & 미삭제 곡만
     */
    private List<TrackLyricsForEmbedding> fetchLyricsChunk(long lastTrackId, int size) {
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
                lastTrackId, size);
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
