package com.fivefy.domain.like.entity;

import static com.fivefy.common.util.ValidationUtils.validateNonNull;
import com.fivefy.common.entity.BaseEntity;
import com.fivefy.domain.like.enums.TargetType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "likes",
        indexes = {
                @Index(name = "idx_likes_user_id", columnList = "user_id") //
        },
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "target_id", "target_type"})
        }
)
@NoArgsConstructor (access = AccessLevel.PROTECTED)
public class Like extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column (nullable = false)
    private Long userId;

    @Column (nullable = false)
    private Long targetId;

    @Column (nullable = false)
    @Enumerated(EnumType.STRING)
    private TargetType targetType;

    public static Like create(Long userId, Long targetId, TargetType targetType) {
        validateNonNull(userId, "userId");
        validateNonNull(targetId, "targetId");
        validateNonNull(targetType, "targetType");

        Like like = new Like();
        like.userId = userId;
        like.targetId = targetId;
        like.targetType = targetType;

        return like;
    }
}
