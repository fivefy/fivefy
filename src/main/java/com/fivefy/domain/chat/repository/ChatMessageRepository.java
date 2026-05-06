package com.fivefy.domain.chat.repository;

import com.fivefy.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    /**
     * 세션의 최근 N개 메시지 (요약 이후 분만).
     * - afterId가 null이면 전체
     * - 있으면 그 다음 message_id부터
     *
     * 결과는 id DESC라 호출자가 reverse해서 시간순으로 사용.
     */
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
