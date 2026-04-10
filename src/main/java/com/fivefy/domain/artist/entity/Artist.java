package com.fivefy.domain.artist.entity;

import com.fivefy.common.entity.BaseEntity;
import com.fivefy.domain.artist.enums.ArtistStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.Objects;

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

    public static Artist create(
            Long ownerUserId,
            String name,
            String bio,
            String profileImageUrl
    ) {
        validateNonNull(ownerUserId, "ownerUserId");
        validateNonNull(name, "name");

        Artist artist = new Artist();

        artist.ownerUserId = ownerUserId;
        artist.name = name;
        artist.bio = bio;
        artist.profileImageUrl = profileImageUrl;
        artist.status = ArtistStatus.ACTIVE;

        return artist;
    }
}