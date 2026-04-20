package com.fivefy.domain.track.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 트랙 등록 신청 거절 요청 DTO
 */
public record TrackApplicationRejectRequest(
        @NotBlank(message = "거절 사유는 필수입니다")
        @Size(max = 255, message = "거절 사유는 255자 이하여야 합니다")
        String rejectionReason
) {
}