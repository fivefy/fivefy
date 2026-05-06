package com.fivefy.ai.controller;

import com.fivefy.ai.dto.ChatSendMessageRequest;
import com.fivefy.ai.dto.ChatStreamEvent;
import com.fivefy.ai.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

/**
 * 챗봇 SSE API.
 *
 * 클라이언트는 EventSource API로 구독:
 *   const es = new EventSource('/api/chat/messages');
 *   es.addEventListener('TEXT', (e) => append(JSON.parse(e.data)));
 *   es.addEventListener('TRACKS', (e) => showCards(JSON.parse(e.data)));
 *   es.addEventListener('DONE', (e) => es.close());
 *
 * 주의: 실제로는 EventSource는 GET만 지원하므로
 *   - 옵션 A: POST + fetch streaming (modern, 권장)
 *   - 옵션 B: GET + 쿼리 파라미터 (메시지 길면 곤란)
 *   여기서는 옵션 A 가정 (POST).
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    /**
     * POST /api/ai/chat/messages
     *
     * 요청:
     *   { "sessionId": null, "message": "잠 안 올 때 뭐 들어?" }
     *
     * 응답: text/event-stream
     *   event: SESSION
     *   data: 42
     *
     *   event: TRACKS
     *   data: [{"trackId": 1, "title": "...", ...}, ...]
     *
     *   event: TEXT
     *   data: "잠"
     *
     *   event: TEXT
     *   data: " 안 올 때는"
     *   ...
     *
     *   event: DONE
     *   data: 1234
     */
    @PostMapping(value = "/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> sendMessage(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ChatSendMessageRequest request) {

        return chatService.sendMessage(userId, request.sessionId(), request.message())
                .map(this::toSseEvent)
                .onErrorResume(e -> {
                    log.error("SSE error", e);
                    return Flux.just(toSseEvent(ChatStreamEvent.error("서버 오류")));
                })
                // Heartbeat: 연결 유지 (proxy/LB가 idle 끊는 걸 방지)
                .mergeWith(heartbeat());
    }

    private ServerSentEvent<String> toSseEvent(ChatStreamEvent event) {
        String json = (event.data() instanceof String s) ? s : objectMapper.writeValueAsString(event.data());
        return ServerSentEvent.<String>builder()
                .event(event.type().name())
                .data(json)
                .build();
    }

    /**
     * 30초마다 comment-only event 전송 (클라이언트엔 보이지 않음).
     * 일부 프록시/LB가 60초 이상 idle 연결을 끊는 걸 방지.
     */
    private Flux<ServerSentEvent<String>> heartbeat() {
        return Flux.interval(Duration.ofSeconds(30))
                .map(i -> ServerSentEvent.<String>builder()
                        .comment("ping")
                        .build())
                .takeUntilOther(Flux.never()); // 메인 스트림 종료까지만
    }
}
