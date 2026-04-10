package com.fivefy.domain.artist.entity;

import com.fivefy.common.entity.BaseEntity;
import com.fivefy.domain.artist.enums.ArtistStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "artists")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Artist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

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

    /**
     * Create a new Artist entity initialized with the provided values and an ACTIVE status.
     *
     * @param ownerUserId     the ID of the user who owns this artist
     * @param name            the artist's name
     * @param bio             the artist's biography or description (may be null)
     * @param profileImageUrl URL of the artist's profile image (may be null)
     * @return                a new, not-yet-persisted Artist instance populated with the given values and `ArtistStatus.ACTIVE`
     */
    public static Artist create(
            Long ownerUserId,
            String name,
            String bio,
            String profileImageUrl
    ) {
        Artist artist = new Artist();

        artist.ownerUserId = ownerUserId;
        artist.name = name;
        artist.bio = bio;
        artist.profileImageUrl = profileImageUrl;
        artist.status = ArtistStatus.ACTIVE;

        return artist;
    }
}