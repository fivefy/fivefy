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
    private LocalDateTime startedAt;

    @Column(nullable = false)
    private LocalDateTime lastPlayedAt;

    private LocalDateTime endedAt;

    public static Playback create(Long trackId, Long userId, String sessionId, String deviceId) {
        return create(trackId, userId, sessionId, deviceId, LocalDateTime.now());
    }

    public static Playback create(Long trackId, Long userId, String sessionId, String deviceId, LocalDateTime now) {
        ValidationUtils.validateNonNull(trackId, "trackId");
        ValidationUtils.validateNonNull(userId, "userId");
        ValidationUtils.validateNonNull(sessionId, "sessionId");
        ValidationUtils.validateNonNull(now, "now");

        Playback playback = new Playback();
        playback.trackId = trackId;
        playback.userId = userId;
        playback.sessionId = sessionId;
        playback.deviceId = deviceId;
        playback.status = PlaybackStatus.PLAYING;
        playback.playedDuration = 0;
        playback.startedAt = now;
        playback.lastPlayedAt = now;

        return playback;
    }

    public boolean isOwner(Long userId) {
        return Objects.equals(this.userId, userId);
    }

    public boolean isPaused() {
        return this.status == PlaybackStatus.PAUSED;
    }

    public boolean isPlaying() {
        return this.status == PlaybackStatus.PLAYING;
    }

    public boolean isEnded() {
        return this.status == PlaybackStatus.STOPPED
                || this.status == PlaybackStatus.SKIPPED
                || this.status == PlaybackStatus.COMPLETED;
    }

    public void resume() {
        resume(LocalDateTime.now());
    }

    public void resume(LocalDateTime now) {
        ValidationUtils.validateNonNull(now, "now");

        if (this.status != PlaybackStatus.PAUSED) {
            throw new BusinessException(PlaybackErrorCode.INVALID_PLAYBACK_STATE);
        }

        this.status = PlaybackStatus.PLAYING;
        this.lastPlayedAt = now;
    }

    public void pause() {
        pause(LocalDateTime.now());
    }

    public void pause(LocalDateTime now) {
        validatePlayingState();
        accumulatePlayedDuration(now);
        this.status = PlaybackStatus.PAUSED;
    }

    public void skip() {
        skip(LocalDateTime.now());
    }

    public void skip(LocalDateTime now) {
        validateSkippableState();

        if (this.status == PlaybackStatus.PLAYING) {
            accumulatePlayedDuration(now);
        }

        this.status = PlaybackStatus.SKIPPED;
        this.endedAt = now;
    }

    public void stop() {
        stop(LocalDateTime.now());
    }

    public void stop(LocalDateTime now) {
        validateSkippableState();

        if (this.status == PlaybackStatus.PLAYING) {
            accumulatePlayedDuration(now);
        }

        this.status = PlaybackStatus.STOPPED;
        this.endedAt = now;
    }

    public void complete() {
        complete(LocalDateTime.now());
    }

    public void complete(LocalDateTime now) {
        ValidationUtils.validateNonNull(now, "now");

        if (this.status != PlaybackStatus.PLAYING) {
            throw new BusinessException(PlaybackErrorCode.INVALID_PLAYBACK_STATE);
        }

        accumulatePlayedDuration(now);
        this.status = PlaybackStatus.COMPLETED;
        this.endedAt = now;
    }

    private void validatePlayingState() {
        if (this.status != PlaybackStatus.PLAYING) {
            throw new BusinessException(PlaybackErrorCode.INVALID_PLAYBACK_STATE);
        }
    }

    private void validateSkippableState() {
        if (this.status != PlaybackStatus.PLAYING && this.status != PlaybackStatus.PAUSED) {
            throw new BusinessException(PlaybackErrorCode.INVALID_PLAYBACK_STATE);
        }
    }

    private void accumulatePlayedDuration(LocalDateTime now) {
        ValidationUtils.validateNonNull(now, "now");

        long seconds = Duration.between(this.lastPlayedAt, now).getSeconds();
        if (seconds > 0) {
            this.playedDuration += (int) seconds;
        }
        this.lastPlayedAt = now;
    }
}
