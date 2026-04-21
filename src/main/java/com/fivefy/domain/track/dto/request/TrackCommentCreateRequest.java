package com.fivefy.domain.track.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 트랙 댓글 작성 요청 DTO
 */
public record TrackCommentCreateRequest(
        @NotBlank(message = "댓글 내용은 필수입니다")
        @Size(max = 1000, message = "댓글은 1000자 이하여야 합니다")
        String content
) {
}