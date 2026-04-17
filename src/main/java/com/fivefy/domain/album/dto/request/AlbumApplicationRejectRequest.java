package com.fivefy.domain.album.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 앨범 등록 신청 거절 요청 DTO
 */
public record AlbumApplicationRejectRequest(

        @NotBlank(message = "거절 사유는 필수입니다.")
        @Size(max = 255, message = "거절 사유는 255자 이하여야 합니다.")
        String rejectionReason
) {
}