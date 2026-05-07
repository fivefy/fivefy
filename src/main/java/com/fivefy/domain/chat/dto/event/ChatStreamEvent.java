package com.fivefy.domain.chat.dto.event;

import com.fivefy.ai.dto.etc.RetrievedTrack;
import com.fivefy.domain.chat.enums.ChatStreamEventType;

import java.util.List;

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
