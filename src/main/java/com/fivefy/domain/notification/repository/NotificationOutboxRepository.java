package com.fivefy.domain.notification.repository;

import com.fivefy.domain.notification.entity.NotificationOutbox;
import com.fivefy.domain.notification.enums.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {

    List<NotificationOutbox> findAllByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);
}
