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
@Table(name = "playlists")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Playlist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String title;

    private String description;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    public static Playlist create(Long userId, String title, String description) {
        validateNonNull(userId, "userId");
        if (title == null || title.isBlank()) {
            throw new BusinessException(PlaylistErrorCode.INVALID_TITLE);
        }

        Playlist playlist = new Playlist();
        playlist.userId = userId;
        playlist.title = title;
        playlist.description = description;

        return playlist;
    }

    public boolean isOwner(Long userId) {
        return Objects.equals(this.userId, userId);
    }

    public void update(String title, String description) {
        if (title == null || title.isBlank()) {
            throw new BusinessException(PlaylistErrorCode.INVALID_TITLE);
        }

        this.title = title;
        this.description = description;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
