package com.fivefy.ai.service;

import com.fivefy.domain.chat.entity.ChatSession;
import com.fivefy.domain.chat.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatSessionPersistService {

    private final ChatSessionRepository sessionRepository;

    @Transactional
    public void updateSummary(Long sessionId, String newSummary, Long lastMessageId) {
        ChatSession session = sessionRepository.findById(sessionId).orElseThrow(
                () -> new IllegalArgumentException("TODO")
        );

        session.updateSummary(newSummary, lastMessageId);
    }
}
