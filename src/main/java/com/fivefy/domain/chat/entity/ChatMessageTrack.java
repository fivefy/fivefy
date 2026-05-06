package com.fivefy.domain.chat.entity;

import groovy.transform.EqualsAndHashCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

import static com.fivefy.common.util.ValidationUtils.validateNonNull;

@Getter
@Entity
@Table(name = "chat_message_tracks")
@IdClass(ChatMessageTrack.PK.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageTrack {

    @Id
    private Long messageId;

    @Id
    private Long trackId;

    @Column(nullable = false)
    private Integer displayOrder;

    public static ChatMessageTrack create(Long messageId, Long trackId, Integer displayOrder) {
        validateNonNull(messageId, "messageId");
        validateNonNull(trackId, "trackId");
        validateNonNull(displayOrder, "displayOrder");

        ChatMessageTrack m = new ChatMessageTrack();
        m.messageId = messageId;
        m.trackId = trackId;
        m.displayOrder = displayOrder;

        return m;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class PK implements Serializable {
        private Long messageId;
        private Long trackId;
    }
}
