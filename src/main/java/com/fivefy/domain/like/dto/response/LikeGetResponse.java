package com.fivefy.domain.like.dto.response;

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
}
