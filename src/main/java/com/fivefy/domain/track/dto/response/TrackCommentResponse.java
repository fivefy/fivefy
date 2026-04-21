package com.fivefy.domain.track.dto.response;

import com.fivefy.domain.track.entity.TrackComment;

import java.time.LocalDateTime;

/**
 * 트랙 댓글 작성 응답 DTO
 */
public record TrackCommentResponse(
        Long commentId,
        Long userId,
        Long trackId,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /**
     * TrackComment 엔티티 → 응답 DTO 변환
     */
    public static TrackCommentResponse from(TrackComment trackComment) {
        return new TrackCommentResponse(
                trackComment.getId(),
                trackComment.getUserId(),
                trackComment.getTrackId(),
                trackComment.getContent(),
                trackComment.getCreatedAt(),
                trackComment.getUpdatedAt()
        );
    }
}