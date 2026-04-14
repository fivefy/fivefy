package com.fivefy.domain.like.dto.response;

import com.fivefy.domain.like.entity.Like;
import com.fivefy.domain.like.enums.TargetType;

import java.time.LocalDateTime;

public record LikeGetResponse (

        Long id,
        Long targetId,
        TargetType targetType,
        String targetName,
        String artistName,
        LocalDateTime createdAt
){
    public static LikeGetResponse from(Like like, String targetName, String artistName) {
        return new LikeGetResponse(
                like.getId(),
                like.getTargetId(),
                like.getTargetType(),
                targetName,
                artistName,
                like.getCreatedAt()
        );
    }
}
