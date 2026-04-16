package com.fivefy.domain.artist.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 아티스트 등록 요청 거절 API 요청 DTO
 */
public record ArtistApplicationRejectRequest(
        @NotBlank(message = "거절 사유는 필수입니다")
        @Size(max = 255, message = "거절 사유는 255자 이하입니다")
        String rejectionReason
) {
}