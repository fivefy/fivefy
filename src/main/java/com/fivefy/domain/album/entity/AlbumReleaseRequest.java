package com.fivefy.domain.album.entity;

import com.fivefy.common.entity.BaseEntity;
import com.fivefy.domain.artist.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Entity
@Table(name = "album_release_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlbumReleaseRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requester_user_id", nullable = false)
    private Long requesterUserId;

    @Column(name = "artist_id", nullable = false)
    private Long artistId;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image_url", length = 255)
    private String coverImageUrl;

    @Column(name = "release_at", nullable = false)
    private LocalDateTime releaseAt;

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

    public static AlbumReleaseRequest create(
            Long requesterUserId,
            Long artistId,
            String title,
            String description,
            String coverImageUrl,
            LocalDateTime releaseAt,
            LocalDateTime scheduledPublishAt
    ) {
        validateNonNull(requesterUserId, "requesterUserId");
        validateNonNull(artistId, "artistId");
        validateNonNull(title, "title");
        validateNonNull(releaseAt, "releaseAt");

        AlbumReleaseRequest request = new AlbumReleaseRequest();

        request.requesterUserId = requesterUserId;
        request.artistId = artistId;
        request.title = title;
        request.description = description;
        request.coverImageUrl = coverImageUrl;
        request.releaseAt = releaseAt;
        request.scheduledPublishAt = scheduledPublishAt;
        request.status = ApplicationStatus.PENDING;

        return request;
    }
}