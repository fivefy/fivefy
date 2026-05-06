package com.fivefy.domain.chat.entity;

import com.fivefy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

import static com.fivefy.common.util.ValidationUtils.validateNonNull;

@Getter
@Entity
@Table(name = "chat_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private Long summaryUntilMessageId;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public static ChatSession create(Long userId) {
        validateNonNull(userId, "userId");

        ChatSession s = new ChatSession();
        s.userId = userId;

        return s;
    }

    public void updateSummary(String summary, Long untilMessageId) {
        this.summary = summary;
        this.summaryUntilMessageId = untilMessageId;
    }

    public void setTitleIfEmpty(String title) {
        if (this.title == null || this.title.isBlank()) {
            this.title = title.length() > 200 ? title.substring(0, 200) : title;
        }
    }
}
