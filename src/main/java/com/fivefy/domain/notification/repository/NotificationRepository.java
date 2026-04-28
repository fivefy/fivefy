package com.fivefy.domain.notification.repository;

import com.fivefy.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findAllByUserId(Long userId, Pageable pageable);

    Long countByUserIdAndReadAtIsNull(Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = CURRENT_TIMESTAMP WHERE n.userId = :userId AND n.readAt IS NULL")
    Integer markAllAsRead(@Param("userId") Long userId);

    void deleteAllByUserId(Long userId);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.id > :lastEventId ORDER BY n.id ASC")
    List<Notification> findMissedNotifications(
            @Param("userId") Long userId,
            @Param("lastEventId") Long lastEventId
    );
}
