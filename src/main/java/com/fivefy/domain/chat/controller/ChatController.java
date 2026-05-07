package com.fivefy.domain.chat.controller;

import com.fivefy.domain.chat.dto.request.ChatSendMessageRequest;
import com.fivefy.domain.chat.dto.event.ChatStreamEvent;
import com.fivefy.domain.chat.service.ChatService;
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

@Slf4j
@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;

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

    private Flux<ServerSentEvent<String>> heartbeat() {
        return Flux.interval(Duration.ofSeconds(30))
                .map(i -> ServerSentEvent.<String>builder()
                        .comment("ping")
                        .build())
                .takeUntilOther(Flux.never()); // 메인 스트림 종료까지만
    }
}
