package com.fivefy.ai.job;

import com.fivefy.ai.dto.TrackForEmbedding;
import com.fivefy.ai.service.TrackEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 매일 새벽 트랙 임베딩 갱신 잡.
 *
 * 설계:
 *  - Track 테이블이 클 수 있으므로 페이징으로 청크 단위 처리 (메모리 보호)
 *  - 청크 단위로 배치 호출 (요청 횟수 최소화)
 *  - 실패해도 다음 청크는 계속 진행 (한 청크 실패가 전체 잡을 죽이지 않음)
 *  - 무한루프 가드 (MAX_ITERATIONS) 로 새벽 3시 사고 방지
 *
 * 왜 Spring Batch가 아닌 단순 @Scheduled 인가:
 *  - Spring Batch는 재시작/복구가 강력하지만 설정 부담이 큼
 *  - 임베딩은 "다음에 다시 돌리면 그만"인 멱등 작업이라 단순 스케줄러로 충분
 *  - 규모가 커지면 그때 Spring Batch로 전환
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fivefy.ai.embedding.enabled", havingValue = "true")
public class TrackEmbeddingJob {

    /**
     * 무한루프 방지 상한.
     * batch-size=100 기준 1,000,000 곡까지 커버. 그 이상은 알람 띄우고 종료.
     */
    private static final int MAX_ITERATIONS = 10_000;

    private final TrackEmbeddingService embeddingService;

    // primary DataSource (MySQL)에서 Track 메타데이터 읽어오기
    // @Primary 덕에 @Qualifier 불필요
    private final JdbcTemplate primaryJdbcTemplate;

    @Value("${fivefy.ai.embedding.batch-size:100}")
    private int batchSize;

    /**
     * 매일 새벽 3시 실행 (cron은 application.yml에서 주입).
     *
     * 실제 운영에서는 Quartz/Airflow 같은 외부 스케줄러를 쓰는 게 좋지만,
     * 1인 프로젝트 수준에서는 @Scheduled로 충분.
     */
    @Scheduled(cron = "${fivefy.ai.embedding.cron}")
    public void runDailyEmbedding() {
        log.info("=== Daily track embedding job started ===");
        long startMs = System.currentTimeMillis();

        int totalProcessed = 0;
        int totalSkipped = 0;
        int totalFailed = 0;
        long lastTrackId = 0;
        int iteration = 0;

        while (iteration++ < MAX_ITERATIONS) {
            List<TrackForEmbedding> chunk = fetchTrackChunk(lastTrackId, batchSize);
            if (chunk.isEmpty()) break;

            long beforeChunkLastId = lastTrackId;
            try {
                var result = embeddingService.embedBatch(chunk);
                totalProcessed += result.processed();
                totalSkipped += result.skipped();
                totalFailed += result.failed();
            } catch (Exception e) {
                log.error("Chunk embedding failed (lastTrackId={}), continuing", lastTrackId, e);
                totalFailed += chunk.size();
            }

            lastTrackId = chunk.get(chunk.size() - 1).trackId();

            // 안전장치: lastTrackId가 전혀 진척 없으면 동일 청크 무한반복 위험 → 중단
            if (lastTrackId <= beforeChunkLastId) {
                log.error("lastTrackId did not advance ({} -> {}), aborting to prevent loop",
                        beforeChunkLastId, lastTrackId);
                break;
            }
        }

        if (iteration >= MAX_ITERATIONS) {
            log.error("Hit MAX_ITERATIONS ({}). Job aborted; some tracks may be unprocessed.",
                    MAX_ITERATIONS);
        }

        long elapsedSec = (System.currentTimeMillis() - startMs) / 1000;
        log.info("=== Daily embedding done in {}s: processed={}, skipped={}, failed={}, iterations={} ===",
                elapsedSec, totalProcessed, totalSkipped, totalFailed, iteration);
    }

    /**
     * track_id > lastTrackId 인 트랙을 size 만큼 읽어옴.
     * OFFSET 기반 페이징은 큰 테이블에서 느려지므로 cursor pagination 사용.
     *
     * 컬럼명/테이블명은 실제 fivefy 스키마에 맞춰서 수정 필요.
     */
    private List<TrackForEmbedding> fetchTrackChunk(long lastTrackId, int size) {
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
                lastTrackId, size);
    }
}