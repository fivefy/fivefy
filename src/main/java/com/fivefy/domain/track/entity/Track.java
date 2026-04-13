package com.fivefy.domain.track.entity;

import com.fivefy.common.entity.BaseEntity;
import static com.fivefy.common.util.ValidationUtils.validateNonNull;
import com.fivefy.domain.track.enums.TrackStatus;
import com.fivefy.domain.track.enums.TrackType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "tracks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Track extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrackType trackType;

    private Long artistId;

    private Long albumId;

    private Long trackNumber;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String lyrics;

    @Column(nullable = false, length = 100)
    private String genre;

    @Column(nullable = false, length = 255)
    private String audioUrl;

    @Column(nullable = false)
    private Long durationSec;

    @Column(length = 255)
    private String featuredArtistText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrackStatus status;

    private LocalDateTime scheduledPublishAt;

    private LocalDateTime publishedAt;

    @Column(nullable = false)
    private Long playCount;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    public static Track create(Long ownerUserId, TrackType trackType, String title, String genre, String audioUrl, Long durationSec) {
        validateNonNull(ownerUserId, "ownerUserId");
        validateNonNull(trackType, "trackType");
        validateNonNull(title, "title");
        validateNonNull(genre, "genre");
        validateNonNull(audioUrl, "audioUrl");
        validateNonNull(durationSec, "durationSec");

        Track track = new Track();
        track.ownerUserId = ownerUserId;
        track.trackType = trackType;
        track.title = title;
        track.genre = genre;
        track.audioUrl = audioUrl;
        track.durationSec = durationSec;

        track.status = TrackStatus.UNPUBLISHED;
        track.playCount = 0L;

        return track;
    }
}
