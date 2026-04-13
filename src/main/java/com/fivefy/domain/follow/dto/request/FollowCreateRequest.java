package com.fivefy.domain.follow.dto.request;

import jakarta.validation.constraints.NotNull;

public record FollowCreateRequest(

        @NotNull (message = "artistId(은)는 필수 입니다")
        Long artistId
) {}
