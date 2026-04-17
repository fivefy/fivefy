package com.fivefy.domain.album.dto.response;

import com.fivefy.domain.album.entity.AlbumApplication;

import java.time.LocalDateTime;

/**
 * 앨범 등록 요청 승인 응답 DTO
 */
public record AlbumApplicationApproveResponse(
        Long requestId,
        Long albumId,
        String status,
        Long reviewedByAdminId,
        LocalDateTime reviewedAt
) {
    public static AlbumApplicationApproveResponse from(
            AlbumApplication request,
            Long albumId
    ) {
        return new AlbumApplicationApproveResponse(
                request.getId(),
                albumId,
                request.getStatus().name(),
                request.getReviewedByAdminId(),
                request.getReviewedAt()
        );
    }
}