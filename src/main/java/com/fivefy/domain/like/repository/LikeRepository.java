package com.fivefy.domain.like.repository;

import com.fivefy.domain.like.entity.Like;
import com.fivefy.domain.like.enums.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long>, LikeQueryRepository {

    boolean existsByUserIdAndTargetIdAndTargetType(Long userId, Long targetId, TargetType targetType);
    Optional<Like> findByIdAndUserId(Long likeId, Long userId);
}
