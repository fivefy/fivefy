package com.fivefy.domain.playlist.entity;

import com.fivefy.common.entity.BaseEntity;
import static com.fivefy.common.util.ValidationUtils.validateNonNull;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

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
        validateNonNull(title, "title");

        Playlist playlist = new Playlist();
        playlist.userId = userId;
        playlist.title = title;
        playlist.description = description;

        return playlist;
    }
}
