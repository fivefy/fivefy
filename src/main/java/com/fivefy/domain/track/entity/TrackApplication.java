package com.fivefy.domain.track.entity;

import com.fivefy.common.entity.BaseEntity;
import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.track.enums.TrackApplicationErrorCode;
import com.fivefy.domain.track.enums.TrackErrorCode;
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
@Table(
        name = "track_applications",
        indexes = {
                @Index(name = "idx_track_application_requester_user_id", columnList = "requester_user_id"),
                @Index(name = "idx_track_application_artist_id", columnList = "artist_id"),
                @Index(name = "idx_track_application_album_id", columnList = "album_id"),
                @Index(name = "idx_track_application_status", columnList = "status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrackApplication extends BaseEntity {

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

    /**
     * 트랙 등록 신청 생성
     */
    public static TrackApplication create(
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

        validateDurationSec(durationSec);

        TrackApplication application = new TrackApplication();

        application.requesterUserId = requesterUserId;
        application.trackType = trackType;
        application.artistId = artistId;
        application.albumId = albumId;
        application.trackNumber = trackNumber;
        application.title = title;
        application.lyrics = lyrics;
        application.genre = genre;
        application.audioUrl = audioUrl;
        application.durationSec = durationSec;
        application.featuredArtistText = featuredArtistText;
        application.scheduledPublishAt = scheduledPublishAt;
        application.status = ApplicationStatus.PENDING;

        return application;
    }

    /**
     * 트랙 등록 신청 승인
     */
    public void approve(Long adminId) {
        validateNonNull(adminId, "adminId");
        validatePending();

        this.status = ApplicationStatus.APPROVED;
        this.reviewedByAdminId = adminId;
        this.reviewedAt = LocalDateTime.now();
        this.rejectionReason = null;
    }

    /**
     * 트랙 등록 신청 거절
     */
    public void reject(Long adminId, String rejectionReason) {
        validateNonNull(adminId, "adminId");
        validateNonNull(rejectionReason, "rejectionReason");
        validatePending();

        this.status = ApplicationStatus.REJECTED;
        this.reviewedByAdminId = adminId;
        this.reviewedAt = LocalDateTime.now();
        this.rejectionReason = rejectionReason;
    }

    // 상태 검증 (PENDING만 처리 가능)
    private void validatePending() {
        if (this.status != ApplicationStatus.PENDING) {
            throw new BusinessException(
                    TrackApplicationErrorCode.ERR_TRACK_APPLICATION_ALREADY_PROCESSED
            );
        }
    }

    // 재생 시간 검증
    private static void validateDurationSec(Long durationSec) {
        if (durationSec <= 0L) {
            throw new BusinessException(TrackErrorCode.ERR_INVALID_DURATION_SEC);
        }
    }
}