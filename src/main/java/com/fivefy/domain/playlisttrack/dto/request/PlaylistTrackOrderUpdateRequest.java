package com.fivefy.domain.playlisttrack.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PlaylistTrackOrderUpdateRequest(
        @NotNull(message = "트랙 ID는 필수입니다")
        Long trackId,
        @NotNull(message = "트랙 순서는 필수입니다")
        @Min(value = 1, message = "트랙 순서는 1 이상이어야 합니다")
        Integer position
) {
}
