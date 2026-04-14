package com.fivefy.domain.playlisttrack.dto.request;

import jakarta.validation.constraints.NotNull;

public record PlaylistTrackCreateRequest(
        @NotNull(message = "트랙 ID는 필수입니다")
        Long trackId
) {
}
