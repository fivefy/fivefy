package com.fivefy.domain.album.entity;

import com.fivefy.common.entity.BaseEntity;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.album.enums.AlbumErrorCode;
import com.fivefy.domain.album.enums.AlbumStatus;
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
        name = "albums",
        indexes = {
                @Index(name = "idx_album_artist_id", columnList = "artist_id"),
                @Index(name = "idx_album_status", columnList = "status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Album extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "artist_id", nullable = false)
    private Long artistId;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image_url", length = 255)
    private String coverImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AlbumStatus status;

    @Column(name = "scheduled_publish_at")
    private LocalDateTime scheduledPublishAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "track_count", nullable = false)
    private Long trackCount;

    @Column(name = "total_duration_sec", nullable = false)
    private Long totalDurationSec;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static Album create(
            Long artistId,
            String title,
            String description,
            String coverImageUrl,
            LocalDateTime scheduledPublishAt
    ) {
        validateNonNull(artistId, "artistId");
        validateNonNull(title, "title");

        Album album = new Album();

        album.artistId = artistId;
        album.title = title;
        album.description = description;
        album.coverImageUrl = coverImageUrl;
        album.scheduledPublishAt = scheduledPublishAt;
        album.status = AlbumStatus.UNPUBLISHED;
        album.trackCount = 0L;
        album.totalDurationSec = 0L;

        return album;
    }

    public void publish() {
        validateNotDeleted();

        if (status == AlbumStatus.PUBLISHED) {
            throw new BusinessException(AlbumErrorCode.ERR_ALBUM_ALREADY_PUBLISHED);
        }
        this.status = AlbumStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public void block() {
        validateNotDeleted();

        if (status == AlbumStatus.BLOCKED) {
            throw new BusinessException(AlbumErrorCode.ERR_ALBUM_ALREADY_BLOCKED);
        }
        this.status = AlbumStatus.BLOCKED;
    }

    public void updateTrackSummary(Long trackCount, Long totalDurationSec) {
        validateNonNull(trackCount, "trackCount");
        validateNonNull(totalDurationSec, "totalDurationSec");
        validateNotDeleted();

        if (trackCount < 0L) {
            throw new BusinessException(AlbumErrorCode.ERR_INVALID_TRACK_COUNT);
        }

        if (totalDurationSec < 0L) {
            throw new BusinessException(AlbumErrorCode.ERR_INVALID_TOTAL_DURATION_SEC);
        }

        this.trackCount = trackCount;
        this.totalDurationSec = totalDurationSec;
    }

    public void softDelete() {
        if (this.deletedAt != null) {
            throw new BusinessException(AlbumErrorCode.ERR_ALBUM_ALREADY_DELETED);
        }
        this.deletedAt = LocalDateTime.now();
    }

    private void validateNotDeleted() {
        if (this.deletedAt != null) {
            throw new BusinessException(AlbumErrorCode.ERR_DELETED_ALBUM_CANNOT_BE_UPDATED);
        }
    }
}