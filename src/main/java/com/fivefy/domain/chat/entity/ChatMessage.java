package com.fivefy.domain.chat.entity;

import com.fivefy.domain.chat.enums.ChatMessageRole;
import com.fivefy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.fivefy.common.util.ValidationUtils.validateNonNull;

@Getter
@Entity
@Table(name = "chat_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatMessageRole role;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    public static ChatMessage user(Long sessionId, String content) {
        return create(sessionId, ChatMessageRole.USER, content);
    }

    public static ChatMessage assistant(Long sessionId, String content) {
        return create(sessionId, ChatMessageRole.ASSISTANT, content);
    }

    private static ChatMessage create(Long sessionId, ChatMessageRole role, String content) {
        validateNonNull(sessionId, "sessionId");
        validateNonNull(role, "chatMessageRole");
        validateNonNull(content, "content");

        ChatMessage m = new ChatMessage();
        m.sessionId = sessionId;
        m.role = role;
        m.content = content;

        return m;
    }
}
