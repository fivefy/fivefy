package com.fivefy.domain.notification.repository;

import com.fivefy.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findAllByUserId(Long userId, Pageable pageable);

    Long countByUserIdAndReadAtIsNull(Long userId);
}
