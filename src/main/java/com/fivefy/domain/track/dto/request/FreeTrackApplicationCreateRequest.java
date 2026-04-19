package com.fivefy.domain.track.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 자유 창작 트랙 등록 신청 요청 DTO
 */
public record FreeTrackApplicationCreateRequest(

        @NotBlank(message = "트랙 제목은 필수입니다")
        @Size(max = 150, message = "트랙 제목은 150자 이하여야 합니다")
        String title,

        String lyrics,

        @NotBlank(message = "장르는 필수입니다")
        @Size(max = 100, message = "장르는 100자 이하여야 합니다")
        String genre,

        @NotBlank(message = "오디오 URL은 필수입니다")
        @Size(max = 255, message = "오디오 URL은 255자 이하여야 합니다")
        String audioUrl,

        @NotNull(message = "재생 시간은 필수입니다")
        @Positive(message = "재생 시간은 1초 이상이어야 합니다")
        Long durationSec
) {
}