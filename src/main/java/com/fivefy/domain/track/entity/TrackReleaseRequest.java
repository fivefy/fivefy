package com.fivefy.domain.track.entity;

import com.fivefy.common.entity.BaseEntity;
import com.fivefy.common.enums.ApplicationStatus;
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
@Table(name = "track_release_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrackReleaseRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requester_user_id", nullable = false)
    private Long requesterUserId;

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

    @Column(name = "scheduled_publish_at")
    private LocalDateTime scheduledPublishAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApplicationStatus status;

    @Column(name = "reviewed_by_admin_id")
    private Long reviewedByAdminId;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static TrackReleaseRequest create(
            Long requesterUserId,
            TrackType trackType,
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
        validateNonNull(requesterUserId, "requesterUserId");
        validateNonNull(trackType, "trackType");
        validateNonNull(title, "title");
        validateNonNull(genre, "genre");
        validateNonNull(audioUrl, "audioUrl");
        validateNonNull(durationSec, "durationSec");

        TrackReleaseRequest request = new TrackReleaseRequest();

        request.requesterUserId = requesterUserId;
        request.trackType = trackType;
        request.artistId = artistId;
        request.albumId = albumId;
        request.trackNumber = trackNumber;
        request.title = title;
        request.lyrics = lyrics;
        request.genre = genre;
        request.audioUrl = audioUrl;
        request.durationSec = durationSec;
        request.featuredArtistText = featuredArtistText;
        request.scheduledPublishAt = scheduledPublishAt;
        request.status = ApplicationStatus.PENDING;

        return request;
    }
}