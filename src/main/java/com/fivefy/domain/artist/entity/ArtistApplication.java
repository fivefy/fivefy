package com.fivefy.domain.artist.entity;

import com.fivefy.common.entity.BaseEntity;
import com.fivefy.common.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "artist_applications")
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
            String profileImageUrl
    ) {
        validateNonNull(requesterUserId, "requesterUserId");
        validateNonNull(requestedName, "requestedName");

        ArtistApplication application = new ArtistApplication();

        application.requesterUserId = requesterUserId;
        application.requestedName = requestedName;
        application.bio = bio;
        application.profileImageUrl = profileImageUrl;
        application.status = ApplicationStatus.PENDING;

        return application;
    }
}