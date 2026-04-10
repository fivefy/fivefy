package com.fivefy.domain.playlist.entity;

import com.fivefy.common.entity.BaseEntity;
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
        validateNonNull(userId, title);
        validate(title);

        Playlist playlist = new Playlist();
        playlist.userId = userId;
        playlist.title = title;
        playlist.description = description;

        return playlist;
    }

    private static void validateNonNull(Long userId, String title) {
        Objects.requireNonNull(userId, "userId는 필수 입니다");
        Objects.requireNonNull(title, "title은 필수 입니다");
    }

    private static void validate(String title) {
        if (title.isBlank()) {
            throw new IllegalArgumentException("title은 공백일 수 없습니다");
        }
        if (title.length() > 100){
            throw new IllegalArgumentException("title 길이는 100자 이하 입니다");
        }
    }
}
