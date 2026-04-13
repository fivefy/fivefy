package com.fivefy.domain.follow.entity;

import com.fivefy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.fivefy.common.util.ValidationUtils.validateNonNull;

@Getter
@Entity
@Table(name = "follows")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Follow extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long artistId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Boolean notificationEnabled;

    public static Follow create(Long artistId, Long userId) {
        validateNonNull(artistId, "artistId");
        validateNonNull(userId, "userId");

        Follow follow = new Follow();
        follow.artistId = artistId;
        follow.userId = userId;
        follow.notificationEnabled = true;

        return follow;
    }

    public void toggleNotification() {
        this.notificationEnabled = !this.notificationEnabled;
    }
}
