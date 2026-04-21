package com.fivefy.domain.like.repository;

import com.fivefy.domain.like.dto.response.LikeGetResponse;
import com.fivefy.domain.like.enums.TargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LikeQueryRepository {

    Page<LikeGetResponse> findLikesWithTarget(Long userId, TargetType targetType, Pageable pageable);
}
