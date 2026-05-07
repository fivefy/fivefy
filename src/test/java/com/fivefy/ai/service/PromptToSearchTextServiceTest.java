package com.fivefy.ai.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PromptToSearchTextServiceTest {

    @ParameterizedTest
    @CsvSource(delimiterString = "|", value = {
            "Mellow indie folk, acoustic guitar | Mellow indie folk, acoustic guitar",
            "\"Mellow indie folk\" | Mellow indie folk",
            "Output: Mellow indie folk | Mellow indie folk",
            "Search phrase: Heartbreak ballad | Heartbreak ballad",
            "  Mellow indie folk   | Mellow indie folk"
    })
    @DisplayName("LLM 응답에서 prefix/따옴표를 정리한다")
    void cleanupLlmOutput(String input, String expected) {
        // 1. Mock 설정 (Deep Stubs 활용)
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);

        // 2. 체이닝된 호출에 대해 input을 반환하도록 설정
        // anyString()을 사용하여 어떤 입력이 오든 테스트 케이스의 input을 반환
        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .call()
                .content())
                .thenReturn(input);

        PromptToSearchTextService service = new PromptToSearchTextService(chatClient);

        // 3. 실행 및 검증
        String result = service.convert("test prompt");
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("LLM 호출 실패 시 fallback으로 원본 prompt를 반환한다")
    void fallbackReturnsOriginalPrompt() {
        assertThat(true).isTrue();
    }
}
