package com.fivefy.domain.playback.entity;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.common.util.ValidationUtils;
import com.fivefy.domain.playback.enums.PlaybackErrorCode;
import com.fivefy.domain.playback.enums.PlaybackStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Entity
@Table(
        name = "playbacks",
        indexes = {
                @Index(name = "idx_playback_ended_at", columnList = "ended_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Playback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long playlistId;

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
    private LocalDateTime startedAt;

    @Column(nullable = false)
    private LocalDateTime lastPlayedAt;

    private LocalDateTime endedAt;

    public static Playback create(Long playlistId, Long trackId, Long userId, String sessionId, String deviceId) {
        ValidationUtils.validateNonNull(playlistId, "playlistId");
        ValidationUtils.validateNonNull(trackId, "trackId");
        ValidationUtils.validateNonNull(userId, "userId");
        ValidationUtils.validateNonNull(sessionId, "sessionId");

        Playback playback = new Playback();
        playback.playlistId = playlistId;
        playback.trackId = trackId;
        playback.userId = userId;
        playback.sessionId = sessionId;
        playback.deviceId = deviceId;
        playback.status = PlaybackStatus.PLAYING;
        playback.playedDuration = 0;
        LocalDateTime now = LocalDateTime.now();
        playback.startedAt = now;
        playback.lastPlayedAt = now;

        return playback;
    }

    public boolean isOwner(Long userId) {
        return Objects.equals(this.userId, userId);
    }

    public boolean isPlaying() {
        return this.status == PlaybackStatus.PLAYING;
    }

    public boolean isPaused() {
        return this.status == PlaybackStatus.PAUSED;
    }

    public boolean isStopped() {
        return this.status == PlaybackStatus.STOPPED;
    }

    public boolean isSkipped() {
        return this.status == PlaybackStatus.SKIPPED;
    }

    public boolean isCompleted() {
        return this.status == PlaybackStatus.COMPLETED;
    }

    public void resume() {
        if (!isPaused()) {
            throw new BusinessException(PlaybackErrorCode.INVALID_PLAYBACK_STATE);
        }

        this.status = PlaybackStatus.PLAYING;
        this.lastPlayedAt = LocalDateTime.now();
    }

    public void pause() {
        if (!isPlaying()) {
            throw new BusinessException(PlaybackErrorCode.INVALID_PLAYBACK_STATE);
        }

        accumulatePlayedDuration();
        this.status = PlaybackStatus.PAUSED;
    }

    public void stop() {
        if (!isPlaying() && !isPaused()) {
            throw new BusinessException(PlaybackErrorCode.INVALID_PLAYBACK_STATE);
        }

        if (isPlaying()) {
            accumulatePlayedDuration();
        }

        this.status = PlaybackStatus.STOPPED;
        this.endedAt = LocalDateTime.now();
    }

    public void skip() {
        if (!isPlaying() && !isPaused()) {
            throw new BusinessException(PlaybackErrorCode.INVALID_PLAYBACK_STATE);
        }

        if (isPlaying()) {
            accumulatePlayedDuration();
        }

        this.status = PlaybackStatus.SKIPPED;
        this.endedAt = LocalDateTime.now();
    }

    public void complete() {
        if (!isPlaying()) {
            throw new BusinessException(PlaybackErrorCode.INVALID_PLAYBACK_STATE);
        }

        accumulatePlayedDuration();
        this.status = PlaybackStatus.COMPLETED;
        this.endedAt = LocalDateTime.now();
    }

    private void accumulatePlayedDuration() {
        LocalDateTime now = LocalDateTime.now();
        long seconds = Duration.between(this.lastPlayedAt, now).getSeconds();

        if (seconds > 0) {
            this.playedDuration += (int) seconds;
        }

        this.lastPlayedAt = now;
    }
}
