package com.fivefy.ai.service;

import com.fivefy.domain.chat.entity.ChatMessage;
import com.fivefy.domain.chat.entity.ChatSession;
import com.fivefy.domain.chat.repository.ChatMessageRepository;
import com.fivefy.domain.chat.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 오래된 대화를 요약해서 토큰 비용 관리.
 *
 * 비동기 (@Async):
 *   유저 응답 후 백그라운드에서 처리. 다음 메시지 보낼 때 즉시 사용 가능.
 *
 * 트리거:
 *   ConversationContextBuilder.shouldSummarize() == true 일 때 ChatService에서 호출.
 *
 * 동작:
 *   1) 기존 summary + 미요약 메시지 N개를 합쳐서 LLM에 요약 요청
 *   2) 새 요약본 + 마지막 메시지 ID를 세션에 저장
 *   3) 다음 컨텍스트 빌드 시 이 요약 + 그 이후 N턴만 사용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSummarizer {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    @Qualifier("anthropicChatClient")
    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
            You are a conversation summarizer. Given a music chatbot conversation,
            produce a concise 2-3 sentence summary in the same language as the conversation.
            Focus on:
            - The user's preferences/mood expressed
            - Key tracks or genres recommended
            - Any specific requests
            
            Output ONLY the summary, no preamble.
            """;

    @Async("summarizerExecutor")
    @Transactional
    public void summarizeAsync(Long sessionId) {
        ChatSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) return;

        Long afterId = session.getSummaryUntilMessageId();
        // 미요약 메시지 모두 가져옴 (최대 50개로 안전장치)
        List<ChatMessage> messages = messageRepository.findRecentMessages(
                sessionId, afterId, PageRequest.of(0, 50));

        if (messages.size() < 4) {
            log.debug("Skip summarization, too few messages: sessionId={}", sessionId);
            return;
        }

        Collections.reverse(messages); // 시간순으로

        // 요약 요청 텍스트 조립
        StringBuilder convo = new StringBuilder();
        if (session.getSummary() != null && !session.getSummary().isBlank()) {
            convo.append("Previous summary: ").append(session.getSummary()).append("\n\n");
        }
        convo.append("Recent conversation:\n");
        for (ChatMessage m : messages) {
            convo.append(m.getRole()).append(": ").append(m.getContent()).append("\n");
        }

        try {
            String newSummary = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(convo.toString())
                    .call()
                    .content();

            Long lastMessageId = messages.get(messages.size() - 1).getId();
            session.updateSummary(newSummary, lastMessageId);
            sessionRepository.save(session);

            log.info("Summarized sessionId={}, until messageId={}", sessionId, lastMessageId);
        } catch (Exception e) {
            log.error("Summarization failed for sessionId={}", sessionId, e);
            // 요약 실패해도 다음 메시지 처리에는 지장 없음 (폴백: 그냥 더 긴 컨텍스트)
        }
    }
}
