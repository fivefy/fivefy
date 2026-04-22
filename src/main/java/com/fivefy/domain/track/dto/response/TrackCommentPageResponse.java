package com.fivefy.domain.track.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 트랙 댓글 목록 조회 응답 DTO
 */
public record TrackCommentPageResponse(
        List<Comment> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public record Comment(
            Long commentId,
            Long userId,
            String content,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}