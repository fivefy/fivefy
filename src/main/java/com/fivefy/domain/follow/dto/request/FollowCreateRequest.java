package com.fivefy.domain.follow.dto.request;

public record FollowCreateRequest(

        Long userId,
        Long artistId
) {}
