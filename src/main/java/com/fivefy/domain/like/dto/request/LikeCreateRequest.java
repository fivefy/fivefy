package com.fivefy.domain.like.dto.request;

import com.fivefy.domain.like.enums.TargetType;
import jakarta.validation.constraints.NotNull;

public record LikeCreateRequest (

        @NotNull (message = "targetId(은)는 필수 입니다")
        Long targetId,
        @NotNull (message = "targetType(은)는 필수 입니다")
        TargetType targetType
){
}
