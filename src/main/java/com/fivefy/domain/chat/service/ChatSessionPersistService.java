package com.fivefy.domain.chat.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.chat.entity.ChatSession;
import com.fivefy.domain.chat.enums.ChatSessionErrorCode;
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
                () -> new BusinessException(ChatSessionErrorCode.ERR_SESSION_NOT_FOUND)
        );

        session.updateSummary(newSummary, lastMessageId);
    }
}
