package com.fivefy.domain.playlist.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PlaylistUpdateRequest(

        @NotBlank(message = "플레이리스트 제목은 필수입니다")
        String title,
        String description
) {
}
