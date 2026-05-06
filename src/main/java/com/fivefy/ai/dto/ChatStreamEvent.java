package com.fivefy.ai.dto;

import com.fivefy.domain.chat.enums.ChatStreamEventType;

import java.util.List;

/**
 * SSE로 클라이언트에 보내는 이벤트 타입.
 *
 * - SESSION:  새 세션 ID (신규 대화일 때만, 첫 이벤트)
 * - TEXT:     LLM이 생성한 텍스트 청크
 * - TRACKS:   추천 트랙 카드 (한 번에)
 * - DONE:     스트리밍 종료 (assistant message ID 포함)
 * - ERROR:    오류
 */
public record ChatStreamEvent(
        ChatStreamEventType type,
        Object data
) {
    public static ChatStreamEvent session(Long sessionId) {
        return new ChatStreamEvent(ChatStreamEventType.SESSION, sessionId);
    }
    public static ChatStreamEvent text(String chunk) {
        return new ChatStreamEvent(ChatStreamEventType.TEXT, chunk);
    }
    public static ChatStreamEvent tracks(List<RetrievedTrack> tracks) {
        return new ChatStreamEvent(ChatStreamEventType.TRACKS, tracks);
    }
    public static ChatStreamEvent done(Long assistantMessageId) {
        return new ChatStreamEvent(ChatStreamEventType.DONE, assistantMessageId);
    }
    public static ChatStreamEvent error(String message) {
        return new ChatStreamEvent(ChatStreamEventType.ERROR, message);
    }
}
