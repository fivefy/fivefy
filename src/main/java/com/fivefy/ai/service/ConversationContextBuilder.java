package com.fivefy.ai.service;

import com.fivefy.domain.chat.entity.ChatMessage;
import com.fivefy.domain.chat.entity.ChatSession;
import com.fivefy.domain.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationContextBuilder {

    private final ChatMessageRepository messageRepository;

    private static final int RECENT_TURN_PAIRS = 6;  // 최근 6턴 = 메시지 12개

    public List<Message> build(ChatSession session) {
        List<Message> messages = new ArrayList<>();

        // 1) 누적 요약 (있을 때만)
        if (session.getSummary() != null && !session.getSummary().isBlank()) {
            messages.add(new SystemMessage(
                    "이전 대화 요약:\n" + session.getSummary()));
        }

        // 2) 최근 N턴 — summaryUntilMessageId 이후만
        Long afterId = session.getSummaryUntilMessageId();
        List<ChatMessage> recent = messageRepository.findRecentMessages(
                session.getId(),
                afterId,
                PageRequest.of(0, RECENT_TURN_PAIRS * 2)
        );

        // findRecentMessages는 id DESC로 반환 → 시간순으로 reverse
        Collections.reverse(recent);

        for (ChatMessage m : recent) {
            messages.add(switch (m.getRole()) {
                case USER -> new UserMessage(m.getContent());
                case ASSISTANT -> new AssistantMessage(m.getContent());
            });
        }

        log.debug("컨텍스트 빌드 완료 (sessionId={}): 총 {}개 메시지 (요약본 = {} 포함)",
                session.getId(), messages.size(), session.getSummary() != null);

        return messages;
    }

    public boolean shouldSummarize(ChatSession session) {
        Long afterId = session.getSummaryUntilMessageId();
        long unsummarizedCount = afterId == null
                ? messageRepository.countBySessionId(session.getId())
                : messageRepository.countBySessionIdAndIdGreaterThan(session.getId(), afterId);

        // 미요약 메시지가 24개(=12턴) 넘어가면 요약 갱신 트리거
        return unsummarizedCount >= 24;
    }
}
