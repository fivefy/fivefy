package com.fivefy.domain.notification.repository;

import com.fivefy.domain.notification.enums.NotificationChannel;
import com.fivefy.domain.notification.enums.NotificationStatus;
import com.fivefy.domain.notification.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class NotificationBulkRepository {

    private static final int BATCH_SIZE = 1000;

    private final JdbcTemplate jdbcTemplate;

    /**
     * 대량 알림 Bulk INSERT
     * IDENTITY 전략으로 JPA saveAll() 배치 불가 → JDBC 직접 구현
     *
     * @return 시도한 행 수
     */
    public int bulkInsert(List<Long> userIds, NotificationType type,
                          String content, NotificationChannel channel,
                          NotificationStatus status, Long trackId) {
        if (userIds == null || userIds.isEmpty()) return 0;

        // INSERT IGNORE — idempotency_key UNIQUE 충돌 시 해당 행만 스킵
        String sql = """
                INSERT IGNORE INTO notifications
                    (user_id, type, content, status, channel, created_at, idempotency_key)
                VALUES
                    (?, ?, ?, ?, ?, ?, ?)
                """;

        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        jdbcTemplate.batchUpdate(sql, userIds, BATCH_SIZE,
                (ps, userId) -> {
                    ps.setLong(1, userId);
                    ps.setString(2, type.name());
                    ps.setString(3, content);
                    ps.setString(4, status.name());
                    ps.setString(5, channel.name());
                    ps.setTimestamp(6, now);
                    ps.setString(7, userId + ":PUBLISH_TRACK:" + trackId);
                });

        log.info("Bulk INSERT 시도: {}건", userIds.size());
        return userIds.size();
    }
}
