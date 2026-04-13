package com.fivefy.domain.track.entity;

import com.fivefy.common.entity.BaseEntity;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.track.enums.TrackExceptionEnum;
import com.fivefy.domain.track.enums.TrackStatus;
import com.fivefy.domain.track.enums.TrackType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

import static com.fivefy.common.util.ValidationUtils.validateNonNull;

@Getter
@Entity
@Table(name = "tracks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Track extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "track_type", nullable = false, length = 30)
    private TrackType trackType;

    @Column(name = "artist_id")
    private Long artistId;

    @Column(name = "album_id")
    private Long albumId;

    @Column(name = "track_number")
    private Long trackNumber;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "lyrics", columnDefinition = "TEXT")
    private String lyrics;

    @Column(name = "genre", nullable = false, length = 100)
    private String genre;

    @Column(name = "audio_url", nullable = false, length = 255)
    private String audioUrl;

    @Column(name = "duration_sec", nullable = false)
    private Long durationSec;

    @Column(name = "featured_artist_text", length = 255)
    private String featuredArtistText;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TrackStatus status;

    @Column(name = "scheduled_publish_at")
    private LocalDateTime scheduledPublishAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "play_count", nullable = false)
    private Long playCount;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static Track createFreeCreation(
            Long ownerUserId,
            String title,
            String lyrics,
            String genre,
            String audioUrl,
            Long durationSec,
            LocalDateTime scheduledPublishAt
    ) {
        validateNonNull(ownerUserId, "ownerUserId");
        validateNonNull(title, "title");
        validateNonNull(genre, "genre");
        validateNonNull(audioUrl, "audioUrl");
        validateNonNull(durationSec, "durationSec");

        Track track = new Track();

        track.ownerUserId = ownerUserId;
        track.trackType = TrackType.FREE_CREATION;
        track.title = title;
        track.lyrics = lyrics;
        track.genre = genre;
        track.audioUrl = audioUrl;
        track.durationSec = durationSec;
        track.scheduledPublishAt = scheduledPublishAt;
        track.status = TrackStatus.PUBLISHED;
        track.playCount = 0L;

        return track;
    }

    public static Track createOfficialRelease(
            Long ownerUserId,
            Long artistId,
            Long albumId,
            Long trackNumber,
            String title,
            String lyrics,
            String genre,
            String audioUrl,
            Long durationSec,
            String featuredArtistText,
            LocalDateTime scheduledPublishAt
    ) {
        validateNonNull(ownerUserId, "ownerUserId");
        validateNonNull(artistId, "artistId");
        validateNonNull(albumId, "albumId");
        validateNonNull(trackNumber, "trackNumber");
        validateNonNull(title, "title");
        validateNonNull(genre, "genre");
        validateNonNull(audioUrl, "audioUrl");
        validateNonNull(durationSec, "durationSec");

        if (trackNumber <= 0L) {
            throw new BusinessException(TrackExceptionEnum.ERR_INVALID_TRACK_NUMBER);
        }

        if (durationSec <= 0L) {
            throw new BusinessException(TrackExceptionEnum.ERR_INVALID_DURATION_SEC);
        }

        Track track = new Track();

        track.ownerUserId = ownerUserId;
        track.trackType = TrackType.OFFICIAL_RELEASE;
        track.artistId = artistId;
        track.albumId = albumId;
        track.trackNumber = trackNumber;
        track.title = title;
        track.lyrics = lyrics;
        track.genre = genre;
        track.audioUrl = audioUrl;
        track.durationSec = durationSec;
        track.featuredArtistText = featuredArtistText;
        track.scheduledPublishAt = scheduledPublishAt;
        track.status = TrackStatus.UNPUBLISHED;
        track.playCount = 0L;

        return track;
    }

    public void publish() {
        validateNotDeleted();

        if (this.status == TrackStatus.PUBLISHED) {
            throw new BusinessException(TrackExceptionEnum.ERR_TRACK_ALREADY_PUBLISHED);
        }
        this.status = TrackStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public void block() {
        validateNotDeleted();

        if (this.status == TrackStatus.BLOCKED) {
            throw new BusinessException(TrackExceptionEnum.ERR_TRACK_ALREADY_BLOCKED);
        }
        this.status = TrackStatus.BLOCKED;
    }

    public void increasePlayCount() {
        validateNotDeleted();

        if (this.status != TrackStatus.PUBLISHED) {
            throw new BusinessException(TrackExceptionEnum.ERR_TRACK_NOT_PUBLISHED);
        }
        this.playCount++;
    }

    public void softDelete() {
        if (this.deletedAt != null) {
            throw new BusinessException(TrackExceptionEnum.ERR_TRACK_ALREADY_DELETED);
        }
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isBlocked() {
        return this.status == TrackStatus.BLOCKED;
    }

    public boolean isPublished() {
        return this.status == TrackStatus.PUBLISHED;
    }

    public boolean isOwnedBy(Long ownerUserId) {
        validateNonNull(ownerUserId, "ownerUserId");
        return this.ownerUserId.equals(ownerUserId);
    }

    private void validateNotDeleted() {
        if (this.deletedAt != null) {
            throw new BusinessException(TrackExceptionEnum.ERR_DELETED_TRACK_CANNOT_BE_UPDATED);
        }
    }
}