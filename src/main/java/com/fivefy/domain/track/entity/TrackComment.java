package com.fivefy.domain.track.entity;

import com.fivefy.common.entity.BaseEntity;
import static com.fivefy.common.util.ValidationUtils.validateNonNull;
import com.fivefy.domain.track.enums.CommentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "track_comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrackComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long trackId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommentStatus status;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    public static TrackComment create(Long userId, Long trackId, String content) {
        validateNonNull(userId, "userId");
        validateNonNull(trackId, "trackId");
        validateNonNull(content, "content");

        TrackComment comment = new TrackComment();
        comment.userId = userId;
        comment.trackId = trackId;
        comment.content = content;

        comment.status = CommentStatus.PUBLISHED;

        return comment;
    }
}
