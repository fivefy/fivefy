package com.fivefy.ai.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptToSearchTextService {

    @Qualifier("anthropicChatClient")
    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
            You are a music search query generator. Given a user's natural language description
            (which may be in Korean, English, or mixed), produce a SHORT English search phrase
            describing the musical characteristics that match the description.

            Rules:
            - Output 5-10 English keywords/short phrases separated by commas.
            - Focus on: genre, mood, instrumentation, vocal style, tempo, era.
            - DO NOT mention specific artist names or song titles.
            - DO NOT translate literally. Interpret the *musical meaning*.
            - Output the search phrase ONLY, no explanation, no quotes.

            Examples:
            User: "비 오는 일요일 오후에 듣기 좋은 곡"
            Output: Mellow indie folk, acoustic guitar, melancholic vocals, slow tempo, rainy day vibe, soft female lead

            User: "운동할 때 들을 신나는 곡"
            Output: High-energy electronic dance, driving bass, fast tempo, motivational beats, modern pop production

            User: "헤어진 다음 날 아침"
            Output: Heartbreak ballad, slow piano, emotional vocals, melancholic indie, introspective lyrics, minor key
            """;

    @Retry(name = "anthropicChat")
    @CircuitBreaker(name = "anthropicChat", fallbackMethod = "convertFallback")
    public String convert(String userPrompt) {
        log.debug("프롬프트를 검색 텍스트로 변환 중: {}", userPrompt);

        String result = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userPrompt)
                .call()
                .content();

        // 모델이 가끔 따옴표나 prefix를 붙이는 경우 정리
        return cleanup(result);
    }

    private static String cleanup(String s) {
        if (s == null) return "";
        return s.trim()
                .replaceAll("^[\"']|[\"']$", "")     // 양 끝 따옴표
                .replaceAll("^Output:\\s*", "")      // "Output: " prefix
                .replaceAll("^Search phrase:\\s*", "");
    }

    @SuppressWarnings("unused")
    private String convertFallback(String userPrompt, Throwable t) {
        log.error("LLM 프롬프트 변환 실패: {}", userPrompt, t);

        return userPrompt;
    }

    // ─── ChatClient Bean 정의 ───
    @Configuration
    static class ChatClientConfig {
        @Bean
        ChatClient anthropicChatClient(ChatModel anthropicChatModel) {
            return ChatClient.builder(anthropicChatModel).build();
        }
    }
}
