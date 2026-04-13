package com.fivefy.domain.follow.dto.response;

import com.fivefy.domain.follow.entity.Follow;

public record FollowGetResponse(

        Long id,
        Long artistId,
        Boolean notificationEnabled
) {
    public static FollowGetResponse from(Follow follow) {
        return new FollowGetResponse(
                follow.getId(),
                follow.getArtistId(),
                follow.getNotificationEnabled()
        );
    }
}
