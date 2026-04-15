package com.fivefy.domain.artist.entity;

import com.fivefy.common.entity.BaseEntity;
import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.artist.enums.ArtistApplicationErrorCode;
import com.fivefy.domain.artist.enums.ArtistType;
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
        name = "artist_applications",
        indexes = {
                @Index(name = "idx_artist_application_requester_user_id", columnList = "requester_user_id"),
                @Index(name = "idx_artist_application_status", columnList = "status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArtistApplication extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requester_user_id", nullable = false)
    private Long requesterUserId;

    @Column(name = "requested_name", nullable = false, length = 100)
    private String requestedName;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "profile_image_url", length = 255)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "artist_type", nullable = false, length = 20)
    private ArtistType artistType;

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

    public static ArtistApplication create(
            Long requesterUserId,
            String requestedName,
            String bio,
            String profileImageUrl,
            ArtistType artistType
    ) {
        validateNonNull(requesterUserId, "requesterUserId");
        validateNonNull(requestedName, "requestedName");
        validateNonNull(artistType, "artistType");

        ArtistApplication application = new ArtistApplication();

        application.requesterUserId = requesterUserId;
        application.requestedName = requestedName;
        application.bio = bio;
        application.profileImageUrl = profileImageUrl;
        application.artistType = artistType;
        application.status = ApplicationStatus.PENDING;

        return application;
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
        if (!isPending()) {
            throw new BusinessException(
                    ArtistApplicationErrorCode.ERR_ARTIST_APPLICATION_ALREADY_PROCESSED);
        }
    }

    public boolean isPending() {
        return this.status == ApplicationStatus.PENDING;
    }

    public boolean isApproved() {
        return this.status == ApplicationStatus.APPROVED;
    }

    public boolean isRejected() {
        return this.status == ApplicationStatus.REJECTED;
    }
}