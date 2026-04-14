package com.fivefy.domain.like.repository;

import com.fivefy.domain.like.entity.Like;
import com.fivefy.domain.like.enums.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeRepository extends JpaRepository<Like, Long> {
    boolean existsByUserIdAndTargetIdAndTargetType(Long userId, Long targetId, TargetType targetType);
}
