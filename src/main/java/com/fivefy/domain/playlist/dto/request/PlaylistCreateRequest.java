package com.fivefy.domain.playlist.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaylistCreateRequest(
        @NotBlank(message = "플레이리스트 제목은 필수입니다")
        @Size(max = 100, message = "플레이리스트 제목은 최대 100자까지 가능합니다")
        String title,
        @Size(max = 255, message = "플레이리스트 설명은 최대 255자까지 가능합니다")
        String description
) {
}
