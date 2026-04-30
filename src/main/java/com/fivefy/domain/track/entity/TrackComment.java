package com.fivefy.domain.track.entity;

import com.fivefy.common.entity.BaseEntity;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.track.enums.TrackCommentErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

import static com.fivefy.common.util.ValidationUtils.validateNonNull;

@Getter
@Entity
@Table(
        name = "track_comments",
        indexes = {
                @Index(name = "idx_track_comment_track_id", columnList = "track_id"),
                @Index(name = "idx_track_comment_user_id", columnList = "user_id"),
                @Index(name = "idx_track_comment_track_deleted_created", columnList = "track_id, deleted_at, created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrackComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "track_id", nullable = false)
    private Long trackId;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static TrackComment create(Long userId, Long trackId, String content) {
        validateNonNull(userId, "userId");
        validateNonNull(trackId, "trackId");
        validateNonNull(content, "content");

        validateContentLength(content);

        TrackComment comment = new TrackComment();
        comment.userId = userId;
        comment.trackId = trackId;
        comment.content = content;

        return comment;
    }

    public void updateContent(String content) {
        validateNonNull(content, "content");

        validateNotDeleted();
        validateContentLength(content);

        this.content = content;
    }

    public void softDelete() {
        if (this.deletedAt != null) {
            throw new BusinessException(TrackCommentErrorCode.ERR_TRACK_COMMENT_ALREADY_DELETED);
        }
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isWrittenBy(Long userId) {
        validateNonNull(userId, "userId");

        return this.userId.equals(userId);
    }

    private void validateNotDeleted() {
        if (this.deletedAt != null) {
            throw new BusinessException(TrackCommentErrorCode.ERR_DELETED_TRACK_COMMENT_CANNOT_BE_UPDATED);
        }
    }

    private static void validateContentLength(String content) {
        if (content.length() > 1000) {
            throw new BusinessException(TrackCommentErrorCode.ERR_INVALID_TRACK_COMMENT_LENGTH);
        }
    }
}
