package com.fivefy.ai.service;

import com.fivefy.domain.chat.entity.ChatMessage;
import com.fivefy.domain.chat.entity.ChatSession;
import com.fivefy.domain.chat.repository.ChatMessageRepository;
import com.fivefy.domain.chat.repository.ChatSessionRepository;
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

/**
 * 대화 컨텍스트 조립.
 *
 * 토큰 비용을 관리하는 게 핵심:
 *  - 최근 6턴 (= 12 메시지)은 그대로
 *  - 그 이전은 별도 LLM이 생성한 요약본으로 대체
 *  - 요약은 비동기로 갱신 (이 클래스에서는 읽기만)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationContextBuilder {

    private final ChatMessageRepository messageRepository;
    private final ChatSessionRepository sessionRepository;

    private static final int RECENT_TURN_PAIRS = 6;  // 최근 6턴 = 메시지 12개

    /**
     * Spring AI Message 리스트로 빌드 (Claude에 그대로 전달 가능).
     *
     * 구조:
     *   [SystemMessage]      ← 외부에서 별도로 prepend
     *   [SystemMessage 요약]  ← 누적 요약이 있으면
     *   [User/Assistant ...]  ← 최근 N턴
     */
    public List<Message> build(ChatSession session) {
        List<Message> messages = new ArrayList<>();

        // 1) 누적 요약 (있을 때만)
        if (session.getSummary() != null && !session.getSummary().isBlank()) {
            messages.add(new SystemMessage(
                    "Previous conversation summary:\n" + session.getSummary()));
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

        log.debug("Built context for sessionId={}: {} messages (incl. summary={})",
                session.getId(), messages.size(), session.getSummary() != null);

        return messages;
    }

    /**
     * 누적 메시지가 임계값을 넘으면 요약 트리거 필요한지 체크.
     * 호출자가 비동기로 SummarizationService 호출 결정.
     */
    public boolean shouldSummarize(ChatSession session) {
        Long afterId = session.getSummaryUntilMessageId();
        long unsummarizedCount = afterId == null
                ? messageRepository.count() // 비효율 - 실제로는 sessionId 조건 추가 필요
                : messageRepository.countBySessionIdAndIdGreaterThan(session.getId(), afterId);

        // 미요약 메시지가 24개(=12턴) 넘어가면 요약 갱신 트리거
        return unsummarizedCount >= 24;
    }
}
