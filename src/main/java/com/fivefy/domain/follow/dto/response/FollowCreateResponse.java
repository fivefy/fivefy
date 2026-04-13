package com.fivefy.domain.follow.dto.response;

import com.fivefy.domain.follow.entity.Follow;

import java.time.LocalDateTime;

public record FollowCreateResponse(

        Long id,
        Long artistId,
        Boolean notificationEnabled,
        LocalDateTime createdAt
) {
    public static FollowCreateResponse from(Follow follow) {
        return new FollowCreateResponse(
                follow.getId(),
                follow.getArtistId(),
                follow.getNotificationEnabled(),
                follow.getCreatedAt()
        );
    }
}
