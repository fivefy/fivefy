package com.fivefy.domain.chat.service;

import com.fivefy.ai.observability.AiBusinessMetrics;
import com.fivefy.domain.chat.dto.etc.ChatSetupResult;
import com.fivefy.domain.chat.dto.event.ChatStreamEvent;
import com.fivefy.ai.dto.etc.RetrievedTrack;
import com.fivefy.ai.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatCommandService commandService;
    private final ChatRetrievalService retrievalService;
    private final ConversationContextBuilder contextBuilder;
    private final RagPromptBuilder promptBuilder;
    private final ChatSummarizer summarizer;
    private final CitationExtractor citationExtractor;
    @Qualifier("anthropicChatClient")
    private final ChatClient chatClient;
    private final AiBusinessMetrics metrics;

    private static final int MAX_CARDS_PER_MESSAGE = 8;

    public Flux<ChatStreamEvent> sendMessage(Long userId, Long sessionId, String userMessage) {
        // ─── 1. 세션 확보 + 유저 메시지 저장 (트랜잭션) ───
        ChatSetupResult setup = commandService.setupSession(userId, sessionId, userMessage);

        // ─── 2. Retrieval (블로킹, but 빠름: 200~500ms) ───
        List<RetrievedTrack> retrieved;
        try {
            retrieved = retrievalService.retrieve(userMessage);
        } catch (Exception e) {
            log.error("검색 데이터 추출 실패 for sessionId={}", setup.session().getId(), e);
            return Flux.just(ChatStreamEvent.error("검색 중 오류가 발생했어요. 잠시 후 다시 시도해주세요."));
        }

        // ─── 3. Augmentation ───
        String systemPrompt = promptBuilder.build(retrieved);
        List<Message> contextMessages = contextBuilder.build(setup.session());

        // ─── 4. Generation (스트리밍) ───
        StringBuilder fullResponse = new StringBuilder();

        Flux<ChatStreamEvent> textStream = chatClient.prompt()
                .system(systemPrompt)
                .messages(contextMessages)
                .user(userMessage)
                .stream()
                .content()
                .map(chunk -> {
                    fullResponse.append(chunk);
                    return ChatStreamEvent.text(chunk);
                });

        // ─── 5. 이벤트 합성 ───
        Flux<ChatStreamEvent> result = Flux.empty();

        // 신규 세션이면 SESSION 이벤트 먼저
        if (setup.isNew()) {
            result = result.concatWith(Flux.just(ChatStreamEvent.session(setup.session().getId())));
        }
        // TEXT 청크들 (스트리밍 — 첫 토큰 빠르게 사용자에게)
        result = result.concatWith(textStream);
        // 완료 시점에 인용된 트랙만 추출 → TRACKS 이벤트 → DB 저장 → DONE
        result = result.concatWith(Flux.defer(() -> {
            String responseText = fullResponse.toString();

            // LLM이 실제로 인용한 트랙만 골라냄
            List<RetrievedTrack> citedTracks = citationExtractor.filterCited(retrieved, responseText);

            List<RetrievedTrack> tracksToShow;
            if (citedTracks.isEmpty()) {
                // LLM이 [N] 인용 형식을 어긴 경우 — 카드 미노출
                // 후보 전체 노출 시 텍스트와 카드 불일치 위험
                tracksToShow = List.of();
                metrics.recordCitationParseFailed();
                log.warn("LLM citation parsing failed, sessionId={}, responseLength={}",
                        setup.session().getId(), responseText.length());
            } else {
                tracksToShow = citedTracks;
                metrics.recordCitationParseSuccess(citedTracks.size());

                // citedTracks 리스트도 상한 적용
                if (tracksToShow.size() > MAX_CARDS_PER_MESSAGE) {
                    tracksToShow = tracksToShow.subList(0, MAX_CARDS_PER_MESSAGE);
                }
            }

            Long assistantMessageId = commandService.saveAssistantMessage(
                    setup.session().getId(), responseText, tracksToShow);

            // 비동기 요약 트리거
            if (contextBuilder.shouldSummarize(setup.session())) {
                summarizer.summarizeAsync(setup.session().getId());
            }

            return Flux.just(
                    ChatStreamEvent.tracks(tracksToShow),
                    ChatStreamEvent.done(assistantMessageId)
            );
        }));

        return result.onErrorResume(e -> {
            log.error("채팅 스트림 오류 for sessionId={}", setup.session().getId(), e);
            return Flux.just(ChatStreamEvent.error("응답 생성 중 오류가 발생했어요."));
        });
    }
}
