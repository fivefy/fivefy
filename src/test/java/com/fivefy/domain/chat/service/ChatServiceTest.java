package com.fivefy.domain.chat.service;

import com.fivefy.ai.dto.etc.RetrievedTrack;
import com.fivefy.ai.observability.AiBusinessMetrics;
import com.fivefy.ai.service.CitationExtractor;
import com.fivefy.ai.service.ConversationContextBuilder;
import com.fivefy.ai.service.RagPromptBuilder;
import com.fivefy.domain.chat.dto.etc.ChatSetupResult;
import com.fivefy.domain.chat.dto.event.ChatStreamEvent;
import com.fivefy.domain.chat.entity.ChatSession;
import com.fivefy.domain.chat.enums.ChatStreamEventType;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @InjectMocks
    private ChatService chatService;

    @Mock private ChatCommandService commandService;
    @Mock private ChatRetrievalService retrievalService;
    @Mock private ConversationContextBuilder contextBuilder;
    @Mock private RagPromptBuilder promptBuilder;
    @Mock private ChatSummarizer summarizer;
    @Mock private CitationExtractor citationExtractor;
    @Mock private ChatModel chatModel;
    @Mock private AiBusinessMetrics metrics;

    @BeforeEach
    void setUp() {
        ChatClient chatClient = ChatClient.create(chatModel);

        chatService = new ChatService(
                commandService,
                retrievalService,
                contextBuilder,
                promptBuilder,
                summarizer,
                citationExtractor,
                chatClient,
                metrics
        );
    }

    @Test
    @DisplayName("LLM 응답에 인용이 없을 경우 빈 트랙 리스트를 반환하고 실패 메트릭을 기록한다")
    void shouldReturnEmptyTracksWhenCitationIsMissing() {
        // given
        Long userId = 1L;
        Long sessionId = 100L;
        String userMessage = "추천해줘";
        String mockLlmResponse = "여기 추천 리스트입니다.";
        RetrievedTrack mockTrack = new RetrievedTrack(
                1L,
                "Song Title",
                "Artist Name",
                "Album Name",
                "Pop",
                2024,
                "https://example.com"
        );
        given(promptBuilder.build(anyList())).willReturn("You are a helpful assistant...");
        ChatSession mockSession = org.mockito.Mockito.mock(ChatSession.class);
        given(mockSession.getId()).willReturn(sessionId);
        given(commandService.setupSession(anyLong(), anyLong(), anyString()))
                .willReturn(new ChatSetupResult(mockSession, false));
        List<RetrievedTrack> retrieved = List.of(mockTrack  );
        given(retrievalService.retrieve(anyString())).willReturn(retrieved);
        ChatResponse chatResponse = new ChatResponse(List.of(
                new Generation(new AssistantMessage(mockLlmResponse))
        ));
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.just(chatResponse));
        given(citationExtractor.filterCited(eq(retrieved), eq(mockLlmResponse)))
                .willReturn(List.of());
        given(commandService.saveAssistantMessage(eq(sessionId), eq(mockLlmResponse), anyList()))
                .willReturn(500L);

        // when
        List<ChatStreamEvent> events = chatService.sendMessage(userId, sessionId, userMessage)
                .collectList()
                .block();

        // then
        assertThat(events).isNotNull();
        ChatStreamEvent tracksEvent = events.stream()
                .filter(e -> e.type() == ChatStreamEventType.TRACKS)
                .findFirst()
                .orElseThrow(() -> new AssertionError("TRACKS 이벤트가 누락되었습니다."));
        assertThat((List<?>) tracksEvent.data()).isEmpty();
        verify(metrics).recordCitationParseFailed();
        verify(commandService).saveAssistantMessage(eq(sessionId), eq(mockLlmResponse), argThat(List::isEmpty));
    }

    @Test
    @DisplayName("인용된 트랙이 8개를 초과하면 최대 8개까지만 노출한다")
    void shouldTruncateTracksWhenCitedExceedsLimit() {
        // given
        Long sessionId = 100L;
        String mockLlmResponse = "여기 10곡을 추천합니다 [1]...[10]";
        List<RetrievedTrack> tenTracks = java.util.stream.IntStream.rangeClosed(1, 10)
                .mapToObj(i -> new RetrievedTrack((long) i, "Title" + i, "Artist", "Album", "Pop", 2024, "url"))
                .toList();
        given(promptBuilder.build(anyList())).willReturn("System Prompt");
        ChatSession mockSession = mock(ChatSession.class);
        given(mockSession.getId()).willReturn(sessionId);
        given(commandService.setupSession(anyLong(), anyLong(), anyString()))
                .willReturn(new ChatSetupResult(mockSession, false));
        given(retrievalService.retrieve(anyString())).willReturn(tenTracks);
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(new AssistantMessage(mockLlmResponse))));
        given(chatModel.stream(any(Prompt.class))).willReturn(Flux.just(chatResponse));
        given(citationExtractor.filterCited(eq(tenTracks), eq(mockLlmResponse))).willReturn(tenTracks);
        given(commandService.saveAssistantMessage(eq(sessionId), eq(mockLlmResponse), anyList())).willReturn(500L);

        // when
        List<ChatStreamEvent> events = chatService.sendMessage(1L, sessionId, "10곡 추천해줘")
                .collectList()
                .block();

        // then
        ChatStreamEvent tracksEvent = Objects.requireNonNull(events).stream()
                .filter(e -> e.type() == ChatStreamEventType.TRACKS)
                .findFirst()
                .orElseThrow();
        assertThat(tracksEvent.data())
                .asInstanceOf(InstanceOfAssertFactories.LIST)
                .hasSize(8)
                .first()
                .isInstanceOf(RetrievedTrack.class);
        assertThat(tracksEvent.data())
                .asInstanceOf(InstanceOfAssertFactories.list(RetrievedTrack.class))
                .hasSize(8)
                .element(7)
                .extracting(RetrievedTrack::title)
                .isEqualTo("Title8");
        verify(metrics).recordCitationParseSuccess(10);
        verify(commandService).saveAssistantMessage(eq(sessionId), eq(mockLlmResponse), argThat(list -> list.size() == 8));
    }
}
