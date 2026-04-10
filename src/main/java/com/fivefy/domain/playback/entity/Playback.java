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
    private int playedDuration;

    @Column(nullable = false)
    private LocalDateTime playedAt;

    public static Playback create(Long trackId, Long userId, String sessionId, String deviceId) {
        validateNonNull(trackId, userId, sessionId);

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

    private static void validateNonNull(Long trackId, Long userId, String sessionId) {
        if (trackId == null) {
            throw new IllegalArgumentException("trackId는 필수입니다");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId는 필수입니다");
        }
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId는 필수입니다");
        }
    }
}
