package com.fivefy.domain.album.entity;

import com.fivefy.common.entity.BaseEntity;
import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.album.enums.AlbumReleaseErrorCode;
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
        name = "album_release_requests",
        indexes = {
                @Index(name = "idx_album_release_request_requester_user_id", columnList = "requester_user_id"),
                @Index(name = "idx_album_release_request_artist_id", columnList = "artist_id"),
                @Index(name = "idx_album_release_request_status", columnList = "status")
        }
)
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

    @Column(name = "publish_delay_days", nullable = false)
    private Integer publishDelayDays;

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
            Integer publishDelayDays
    ) {
        validateNonNull(requesterUserId, "requesterUserId");
        validateNonNull(artistId, "artistId");
        validateNonNull(title, "title");
        validateNonNull(publishDelayDays, "publishDelayDays");

        AlbumReleaseRequest request = new AlbumReleaseRequest();

        request.requesterUserId = requesterUserId;
        request.artistId = artistId;
        request.title = title;
        request.description = description;
        request.coverImageUrl = coverImageUrl;
        request.publishDelayDays = publishDelayDays;
        request.status = ApplicationStatus.PENDING;

        return request;
    }

    public void approve(Long adminId) {
        validateNonNull(adminId, "adminId");
        validatePending();

        this.status = ApplicationStatus.APPROVED;
        this.reviewedByAdminId = adminId;
        this.reviewedAt = LocalDateTime.now();
        this.rejectionReason = null;
    }

    public void reject(Long adminId, String rejectionReason) {
        validateNonNull(adminId, "adminId");
        validateNonNull(rejectionReason, "rejectionReason");
        validatePending();

        this.status = ApplicationStatus.REJECTED;
        this.reviewedByAdminId = adminId;
        this.reviewedAt = LocalDateTime.now();
        this.rejectionReason = rejectionReason;
    }

    private void validatePending() {
        if (this.status != ApplicationStatus.PENDING) {
            throw new BusinessException(
                    AlbumReleaseErrorCode.ERR_ALBUM_RELEASE_ALREADY_PROCESSED);
        }
    }
}