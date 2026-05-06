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

/**
 * 사용자 자연어 프롬프트를 음악 검색 최적화용 영어 키워드로 변환하는 서비스입니다.
 *
 * <p><strong>핵심 설계 의도:</strong></p>
 * <ul>
 *   <li><strong>검색 정밀도(Precision) 향상:</strong> Multi-lingual 모델인 <code>bge-m3</code>를 사용함에도 불구하고,
 *       사용자의 문장형 질문에는 검색에 불필요한 조사나 서술어가 포함됩니다. LLM을 통해 이를 음악적 특징(Genre, Mood, Tempo)
 *       위주의 핵심 키워드로 정제하여 검색 결과의 Relevancy를 극대화합니다.</li>
 *   <li><strong>의미론적 보강:</strong> "비 오는 날"과 같은 단순 상황 묘사를 "Mellow, Acoustic, Melancholic" 등
 *       임베딩 공간에서 음악 트랙과 더 가깝게 위치할 수 있는 전문 음악 용어로 확장(Expansion)합니다.</li>
 *   <li><strong>비용 및 성능 최적화:</strong> Claude Haiku 모델을 채택하여 복잡한 추론 대신
 *       빠른 텍스트 정제 기능을 활용함으로써 서비스 레이턴시를 최소화합니다.</li>
 * </ul>
 */
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
        log.debug("Converting prompt to search text: {}", userPrompt);

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
        log.error("LLM conversion failed for prompt: {}", userPrompt, t);
        // 폴백: 원본 prompt를 그대로 사용 (영어 음악 키워드 매칭은 약하지만 에러보단 나음)
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
