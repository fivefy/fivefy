package com.fivefy.domain.artist.entity;

import com.fivefy.common.entity.BaseEntity;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.artist.enums.ArtistErrorCode;
import com.fivefy.domain.artist.enums.ArtistStatus;
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
        name = "artists",
        indexes = {
                @Index(name = "idx_artist_owner_user_id", columnList = "owner_user_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Artist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "artist_type", nullable = false, length = 20)
    private ArtistType artistType;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "profile_image_url", length = 255)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ArtistStatus status;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static Artist create(
            Long ownerUserId,
            String name,
            ArtistType artistType,
            String bio,
            String profileImageUrl
    ) {
        validateNonNull(ownerUserId, "ownerUserId");
        validateNonNull(name, "name");
        validateNonNull(artistType, "artistType");

        Artist artist = new Artist();

        artist.ownerUserId = ownerUserId;
        artist.name = name;
        artist.artistType = artistType;
        artist.bio = bio;
        artist.profileImageUrl = profileImageUrl;
        artist.status = ArtistStatus.ACTIVE;

        return artist;
    }

    public void updateProfile(String bio, String profileImageUrl) {
        validateNotDeleted();

        if (bio != null) {
            this.bio = bio;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
    }

    public void suspend() {
        validateNotDeleted();

        if (this.status == ArtistStatus.SUSPENDED) {
            throw new BusinessException(ArtistErrorCode.ERR_ARTIST_ALREADY_SUSPENDED);
        }
        this.status = ArtistStatus.SUSPENDED;
    }

    public void activate() {
        validateNotDeleted();

        if (this.status == ArtistStatus.ACTIVE) {
            throw new BusinessException(ArtistErrorCode.ERR_ARTIST_ALREADY_ACTIVATED);
        }
        this.status = ArtistStatus.ACTIVE;
    }

    public void softDelete() {
        if (this.deletedAt != null) {
            throw new BusinessException(ArtistErrorCode.ERR_ARTIST_ALREADY_DELETED);
        }
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isOwnedBy(Long userId) {
        validateNonNull(userId, "userId");
        return this.ownerUserId.equals(userId);
    }

    private void validateNotDeleted() {
        if (this.deletedAt != null) {
            throw new BusinessException(ArtistErrorCode.ERR_DELETED_ARTIST_CANNOT_BE_UPDATED);
        }
    }
}