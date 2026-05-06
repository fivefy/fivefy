package com.fivefy.domain.chat.repository;

import com.fivefy.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
        SELECT m FROM ChatMessage m
        WHERE m.sessionId = :sessionId
          AND (:afterId IS NULL OR m.id > :afterId)
        ORDER BY m.id DESC
        """)
    List<ChatMessage> findRecentMessages(
            @Param("sessionId") Long sessionId,
            @Param("afterId") Long afterId,
            Pageable pageable);

    long countBySessionIdAndIdGreaterThan(Long sessionId, Long messageId);

    long countBySessionId(Long id);
}
