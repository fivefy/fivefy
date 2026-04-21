package com.fivefy.domain.user.scheduler;

import com.fivefy.domain.user.enums.UserStatus;
import com.fivefy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserScheduler {

    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final TransactionTemplate transactionTemplate;

    private static final int ANONYMIZE_DAYS = 30;
    private static final int SUSPENDED_DAYS = 30;
    private static final String LAST_ACTIVE_PREFIX  = "lastActive:";
    private static final String LAST_ACTIVE_PATTERN = "lastActive:*";
    private static final String THROTTLE_PREFIX = "lastActive:throttle:";

    // scan 배치 단위 — 한 배치씩 트랜잭션을 끊어서 긴 트랜잭션 방지
    private static final int BATCH_SIZE = 500;

    // 매일 새벽 4시 — 탈퇴 후 30일 경과 유저 개인정보 익명화
    @Scheduled(cron = "0 0 4 * * *")
    @SchedulerLock(name = "anonymizeDeletedUsers", lockAtMostFor = "1h", lockAtLeastFor = "1m")
    @Transactional
    public void anonymizeDeletedUsers() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(ANONYMIZE_DAYS);
        log.info("탈퇴 유저 개인정보 익명화 시작 — 기준일: {}", threshold);

        int anonymized = transactionTemplate.execute(
                status -> userRepository.anonymizeDeletedUsers(threshold)
        );

        log.info("탈퇴 유저 개인정보 익명화 완료 — {}명 처리", anonymized);
    }

    // 1시간마다 Redis lastActive → DB 반영
    @Scheduled(cron = "0 0 * * * *")
    @SchedulerLock(name = "syncLastActiveAt", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    public void syncLastActiveAt() {
        long startedAt = System.currentTimeMillis();
        log.info("lastActiveAt 동기화 시작");

        SyncResult result = flushLastActiveFromRedis();

        long elapsedMs = System.currentTimeMillis() - startedAt;
        log.info("lastActiveAt 동기화 완료 — 성공: {}건, 실패: {}건, 배치: {}개, 소요: {}ms",
                result.updated, result.errors, result.batches, elapsedMs);

        // 임계치 알림 — 10분 lock 대비 경고선 5분
        if (elapsedMs > 5 * 60 * 1000) {
            log.warn("syncLastActiveAt 소요시간 임계 초과 — {}ms", elapsedMs);
        }
        if (result.errors > 0) {
            log.warn("syncLastActiveAt 실패건 발생 — {}건", result.errors);
        }
    }

    // 매일 새벽 3시 — 30일 미접속 유저 SUSPENDED 처리
    @Scheduled(cron = "0 0 3 * * *")
    @SchedulerLock(name = "suspendInactiveUsers", lockAtMostFor = "1h", lockAtLeastFor = "1m")
    public void suspendInactiveUsers() {
        log.info("미접속 유저 정지 처리 시작");

        // 1. Redis 잔여분을 먼저 DB로 flush — 그래야 방금 접속한 유저가 잘못 SUSPENDED되지 않음
        SyncResult syncResult = flushLastActiveFromRedis();
        log.info("선행 동기화 완료 — 성공: {}건, 실패: {}건", syncResult.updated, syncResult.errors);

        // 2. DB의 lastActiveAt 기준으로 SUSPENDED 처리
        LocalDateTime threshold = LocalDateTime.now().minusDays(SUSPENDED_DAYS);
        int suspended = transactionTemplate.execute(status ->
                userRepository.suspendInactiveUsers(
                        threshold, UserStatus.ACTIVE, UserStatus.SUSPENDED
                )
        );

        log.info("미접속 유저 정지 처리 완료 — 기준일: {}, {}명 SUSPENDED", threshold, suspended);
    }

    /**
     * Redis의 lastActive:* 키를 스캔하여 DB에 반영.
     * - 배치 단위로 트랜잭션을 끊어 긴 트랜잭션 방지
     * - GETDEL로 읽기+삭제를 원자적으로 처리 → race condition 제거
     * - throttle 키는 자연 만료에 맡김 (별도 삭제 불필요)
     */
    private SyncResult flushLastActiveFromRedis() {
        int updated = 0;
        int errors = 0;
        int batches = 0;
        List<KeyValue> batch = new ArrayList<>(BATCH_SIZE);

        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions()
                        .match(LAST_ACTIVE_PATTERN)
                        .count(100)
                        .build())) {

            while (cursor.hasNext()) {
                String key = cursor.next();

                // throttle 키는 건너뛰기 (패턴이 겹침: lastActive:throttle:*)
                if (key.startsWith(THROTTLE_PREFIX)) continue;

                // GETDEL — 읽는 순간 삭제까지 원자적으로 수행
                // 이 호출 이후 새로 들어오는 요청은 새 키를 만들고, 다음 sync에서 처리됨
                String value = redisTemplate.opsForValue().getAndDelete(key);
                if (value == null) continue;

                batch.add(new KeyValue(key, value));

                if (batch.size() >= BATCH_SIZE) {
                    BatchResult r = processBatch(batch);
                    updated += r.updated;
                    errors += r.errors;
                    batches++;
                    batch.clear();
                }
            }
        }

        if (!batch.isEmpty()) {
            BatchResult r = processBatch(batch);
            updated += r.updated;
            errors += r.errors;
            batches++;
        }

        return new SyncResult(updated, errors, batches);
    }

    /**
     * 배치 단위로 트랜잭션을 시작해서 DB에 반영.
     * 트랜잭션이 배치 크기만큼만 열려있으므로 connection 점유 최소화.
     */
    private BatchResult processBatch(List<KeyValue> batch) {
        return transactionTemplate.execute(status -> {
            int ok = 0;
            int err = 0;
            for (KeyValue kv : batch) {
                try {
                    Long userId = extractUserId(kv.key);
                    // Instant로 저장했으므로 Instant → LocalDateTime 변환
                    LocalDateTime lastActiveAt = LocalDateTime.ofInstant(
                            Instant.parse(kv.value), ZoneId.systemDefault()
                    );
                    userRepository.updateLastActiveAt(userId, lastActiveAt);
                    ok++;
                } catch (Exception e) {
                    err++;
                    // 파싱 실패 등 복구 불가 키는 이미 GETDEL로 삭제됐으므로
                    // 다음 sync에 다시 시도되지 않음 — 로그만 남기고 넘어감
                    log.warn("lastActiveAt 업데이트 실패 key={} value={} : {}",
                            kv.key, kv.value, e.getMessage());
                }
            }
            return new BatchResult(ok, err);
        });
    }

    private Long extractUserId(String key) {
        return Long.parseLong(key.substring(LAST_ACTIVE_PREFIX.length()));
    }

    private record KeyValue(String key, String value) {}
    private record SyncResult(int updated, int errors, int batches) {}
    private record BatchResult(int updated, int errors) {}
}
