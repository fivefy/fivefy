package com.fivefy.domain.chat.repository;

import com.fivefy.domain.chat.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    Optional<ChatSession> findByIdAndUserId(Long id, Long userId);
}
