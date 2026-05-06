package com.fivefy.ai.service;

import com.fivefy.domain.chat.entity.ChatMessage;
import com.fivefy.domain.chat.entity.ChatMessageTrack;
import com.fivefy.domain.chat.entity.ChatSession;
import com.fivefy.ai.domain.ChatSetupResult;
import com.fivefy.ai.dto.RetrievedTrack;
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

    /**
     * 세션 확보 + 유저 메시지 저장.
     * 트랜잭션이 필요한 부분만 분리.
     */
    @Transactional
    public ChatSetupResult setupSession(Long userId, Long sessionId, String userMessage) {
        ChatSession session;
        boolean isNew = false;

        if (sessionId == null) {
            session = sessionRepository.save(ChatSession.create(userId));
            isNew = true;
        } else {
            session = sessionRepository.findByIdAndUserId(sessionId, userId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "세션을 찾을 수 없습니다: " + sessionId));
        }

        // 유저 메시지 저장
        messageRepository.save(ChatMessage.user(session.getId(), userMessage));

        // 첫 메시지면 제목 설정
        session.setTitleIfEmpty(userMessage);
        sessionRepository.save(session);

        return new ChatSetupResult(session, isNew);
    }

    /**
     * Assistant 응답 + 트랙 카드 저장 (트랜잭션).
     */
    @Transactional
    public Long saveAssistantMessage(Long sessionId, String content, List<RetrievedTrack> tracks) {
        ChatSession session = sessionRepository.findById(sessionId).orElseThrow(
                () -> new IllegalArgumentException("TODO")
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
