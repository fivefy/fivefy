package com.fivefy.domain.recommendation.entity;

import com.fivefy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.fivefy.common.util.ValidationUtils.validateNonNull;

@Getter
@Entity
@Table (name = "recommendations")
@NoArgsConstructor (access = AccessLevel.PROTECTED)
public class Recommendation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long trackId;

    private Long score;

    private String reason;

    public static Recommendation create(Long userId, Long trackId, Long score, String reason) {
        validateNonNull(userId, "userId");
        validateNonNull(trackId, "trackId");

        Recommendation recommendation = new Recommendation();
        recommendation.userId = userId;
        recommendation.trackId = trackId;
        recommendation.score = score;
        recommendation.reason = reason;

        return recommendation;
    }
}
