package com.fivefy.domain.chat.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.chat.entity.ChatMessage;
import com.fivefy.domain.chat.entity.ChatSession;
import com.fivefy.domain.chat.enums.ChatSessionErrorCode;
import com.fivefy.domain.chat.repository.ChatMessageRepository;
import com.fivefy.domain.chat.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSummarizer {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatSessionPersistService sessionPersistService;
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
    public void summarizeAsync(Long sessionId) {
        ChatSession session = sessionRepository.findById(sessionId).orElseThrow(
                () -> new BusinessException(ChatSessionErrorCode.ERR_SESSION_NOT_FOUND)
        );

        Long afterId = session.getSummaryUntilMessageId();
        // 미요약 메시지 모두 가져옴 (최대 50개로 안전장치)
        List<ChatMessage> messages = messageRepository.findRecentMessages(
                sessionId, afterId, PageRequest.of(0, 50));

        if (messages.size() < 4) {
            log.debug("대화 요약 건너뜀 (메시지 부족): sessionId={}", sessionId);
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

            // DB 저장은 짧은 트랜잭션으로 처리
            sessionPersistService.updateSummary(session.getId(), newSummary, lastMessageId);

            log.info("대화 요약 완료: sessionId={}, lastMessageId={}", sessionId, lastMessageId);
        } catch (Exception e) {
            log.error("대화 요약 실패 for sessionId={}", sessionId, e);
            // 요약 실패해도 다음 메시지 처리에는 지장 없음 (폴백: 그냥 더 긴 컨텍스트)
        }
    }
}
