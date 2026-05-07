package com.fivefy.domain.chat.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.chat.entity.ChatMessage;
import com.fivefy.domain.chat.entity.ChatMessageTrack;
import com.fivefy.domain.chat.entity.ChatSession;
import com.fivefy.domain.chat.dto.etc.ChatSetupResult;
import com.fivefy.ai.dto.etc.RetrievedTrack;
import com.fivefy.domain.chat.enums.ChatSessionErrorCode;
import com.fivefy.domain.chat.repository.ChatMessageRepository;
import com.fivefy.domain.chat.repository.ChatMessageTrackRepository;
import com.fivefy.domain.chat.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatCommandService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatMessageTrackRepository messageTrackRepository;

    @Transactional
    public ChatSetupResult setupSession(Long userId, Long sessionId, String userMessage) {
        ChatSession session;
        boolean isNew = false;

        if (sessionId == null) {
            session = sessionRepository.save(ChatSession.create(userId));
            isNew = true;
        } else {
            session = sessionRepository.findByIdAndUserId(sessionId, userId).orElseThrow(
                    () -> new BusinessException(ChatSessionErrorCode.ERR_SESSION_NOT_FOUND)
            );
        }

        // 유저 메시지 저장
        messageRepository.save(ChatMessage.user(session.getId(), userMessage));

        // 첫 메시지면 제목 설정
        session.setTitleIfEmpty(userMessage);
        sessionRepository.save(session);

        return new ChatSetupResult(session, isNew);
    }

    @Transactional
    public Long saveAssistantMessage(Long sessionId, String content, List<RetrievedTrack> tracks) {
        ChatSession session = sessionRepository.findById(sessionId).orElseThrow(
                () -> new BusinessException(ChatSessionErrorCode.ERR_SESSION_NOT_FOUND)
        );

        ChatMessage saved = messageRepository.save(ChatMessage.assistant(session.getId(), content));

        // 트랙 카드 저장 (display_order 순서대로)
        List<ChatMessageTrack> entries = new ArrayList<>(tracks.size());
        for (int i = 0; i < tracks.size(); i++) {
            entries.add(ChatMessageTrack.create(saved.getId(), tracks.get(i).trackId(), i));
        }
        messageTrackRepository.saveAll(entries);

        return saved.getId();
    }
}
