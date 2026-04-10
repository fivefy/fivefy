package com.fivefy.domain.playback.entity;

import com.fivefy.domain.playback.enums.PlaybackStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Entity
@Table(name = "playbacks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Playback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long trackId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String sessionId;

    @Column(length = 100)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlaybackStatus status;

    @Column(nullable = false)
    private Integer playedDuration;

    @Column(nullable = false)
    private LocalDateTime playedAt;

    public static Playback create(Long trackId, Long userId, String sessionId, String deviceId) {
        validateNotNull(trackId, "trackId");
        validateNotNull(userId, "userId");
        validateNotNull(sessionId, "sessionId");

        Playback playback = new Playback();
        playback.trackId = trackId;
        playback.userId = userId;
        playback.sessionId = sessionId;
        playback.deviceId = deviceId;

        playback.status = PlaybackStatus.START;
        playback.playedDuration = 0;
        playback.playedAt = LocalDateTime.now();

        return playback;
    }

    private static void validateNotNull(Object value, String fieldName) {
        Objects.requireNonNull(value, fieldName + "는 필수입니다. (validationNotNull)");
    }
}
