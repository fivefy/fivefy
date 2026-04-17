package com.fivefy.domain.album.dto.response;

import com.fivefy.domain.album.entity.AlbumApplication;

import java.time.LocalDateTime;

/**
 * 앨범 등록 신청 승인 응답 DTO
 */
public record AlbumApplicationApproveResponse(
        Long applicationId,
        Long albumId,
        String status,
        Long reviewedByAdminId,
        LocalDateTime reviewedAt
) {
    public static AlbumApplicationApproveResponse from(
            AlbumApplication application,
            Long albumId
    ) {
        return new AlbumApplicationApproveResponse(
                application.getId(),
                albumId,
                application.getStatus().name(),
                application.getReviewedByAdminId(),
                application.getReviewedAt()
        );
    }
}