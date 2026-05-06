package com.fivefy.ai.service;

import com.fivefy.ai.domain.ChatSetupResult;
import com.fivefy.ai.dto.ChatStreamEvent;
import com.fivefy.ai.dto.RetrievedTrack;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 챗봇 메인 서비스 — RAG + 스트리밍.
 *
 * 처리 순서:
 *  1) 세션 확보 (신규 or 기존)
 *  2) 유저 메시지 즉시 저장 (DB)
 *  3) Retrieval — 트랙 검색
 *  4) Augmentation — 시스템 프롬프트 조립 + 대화 기록
 *  5) Generation — Claude 스트리밍
 *  6) 응답 버퍼링 → 완료 후 ASSISTANT 메시지 + 트랙 카드 저장
 *
 * 스트리밍 전략:
 *   Reactor Flux<StreamEvent> 반환.
 *   컨트롤러에서 SSE로 변환.
 */
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

    /**
     * 메시지 전송 → 스트리밍 응답.
     *
     * 반환되는 Flux는 다음 이벤트들을 차례로 emit:
     *   1) SESSION (신규 세션일 때만)
     *   2) TEXT × N (LLM 응답 청크들 — 첫 토큰 빠르게)
     *   3) TRACKS (LLM이 [N]으로 인용한 트랙만 — 응답 완료 시점)
     *   4) DONE
     *
     *   TRACKS를 응답 완료 후로 미루고, 응답 텍스트에서 [숫자] 인용을 파싱해서
     *   *실제 인용된 트랙만* 카드로 노출. 텍스트와 카드가 100% 일치.
     *   ChatGPT의 References 패턴과 동일.
     */
    public Flux<ChatStreamEvent> sendMessage(Long userId, Long sessionId, String userMessage) {
        // ─── 1. 세션 확보 + 유저 메시지 저장 (트랜잭션) ───
        ChatSetupResult setup = commandService.setupSession(userId, sessionId, userMessage);

        // ─── 2. Retrieval (블로킹, but 빠름: 200~500ms) ───
        List<RetrievedTrack> retrieved;
        try {
            retrieved = retrievalService.retrieve(userMessage);
        } catch (Exception e) {
            log.error("Retrieval failed for sessionId={}", setup.session().getId(), e);
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

            // LLM이 [N] 인용을 안 했거나 매칭 실패 시 fallback: 후보 전체 노출
            // (사용자가 카드 영역을 비어있는 상태로 보면 더 어색함)
            List<RetrievedTrack> tracksToShow = citedTracks.isEmpty() ? retrieved : citedTracks;

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
            log.error("Chat stream error for sessionId={}", setup.session().getId(), e);
            return Flux.just(ChatStreamEvent.error("응답 생성 중 오류가 발생했어요."));
        });
    }
}
