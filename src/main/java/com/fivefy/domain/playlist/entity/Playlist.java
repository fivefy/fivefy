package com.fivefy.domain.playlist.entity;

import com.fivefy.common.entity.BaseEntity;
import static com.fivefy.common.util.ValidationUtils.validateNonNull;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.playlist.enums.PlaylistErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Entity
@Table(
        name = "playlists",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_playlist_user_title_deleted",
                        columnNames = {"user_id", "title", "deleted"}
                )
        },
        indexes = {
                @Index(name = "idx_playlist_deleted_at", columnList = "deleted_at"),
                @Index(name = "idx_playlist_user_deleted_at", columnList = "user_id, deleted_at"),
                @Index(name = "idx_playlist_user_title_deleted_at", columnList = "user_id, title, deleted_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Playlist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean deleted;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static Playlist create(Long userId, String title, String description) {
        validateNonNull(userId, "userId");
        validateTitle(title);

        Playlist playlist = new Playlist();
        playlist.userId = userId;
        playlist.title = title;
        playlist.description = description;
        playlist.deleted = false;

        return playlist;
    }

    public boolean isOwner(Long userId) {
        return Objects.equals(this.userId, userId);
    }

    public boolean isDeleted() { return this.deleted; }

    public void update(String title, String description) {
        validateNotDeleted();
        validateTitle(title);

        this.title = title;
        this.description = description;
    }

    public void delete() {
        validateNotDeleted();
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    private void validateNotDeleted() {
        if (isDeleted()) {
            throw new BusinessException(PlaylistErrorCode.ALREADY_DELETED_PLAYLIST);
        }
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank() || title.length() > 100) {
            throw new BusinessException(PlaylistErrorCode.INVALID_TITLE);
        }
    }
}
