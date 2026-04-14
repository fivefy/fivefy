package com.fivefy.domain.like.dto.response;

import com.fivefy.domain.like.entity.Like;
import com.fivefy.domain.like.enums.TargetType;

import java.time.LocalDateTime;

public record LikeCreateResponse (

        Long id,
        Long targetId,
        TargetType targetType,
        LocalDateTime createdAt
){
    public static LikeCreateResponse from(Like like) {
        return new LikeCreateResponse(
                like.getId(),
                like.getTargetId(),
                like.getTargetType(),
                like.getCreatedAt()
        );
    }
}
