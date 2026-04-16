package com.fivefy.domain.album.dto.response;

import com.fivefy.domain.album.entity.AlbumReleaseRequest;

import java.time.LocalDateTime;

/**
 * 앨범 등록 요청 승인 응답 DTO
 */
public record AlbumReleaseRequestApproveResponse(
        Long requestId,
        Long albumId,
        String status,
        Long reviewedByAdminId,
        LocalDateTime reviewedAt
) {
    public static AlbumReleaseRequestApproveResponse from(
            AlbumReleaseRequest request,
            Long albumId
    ) {
        return new AlbumReleaseRequestApproveResponse(
                request.getId(),
                albumId,
                request.getStatus().name(),
                request.getReviewedByAdminId(),
                request.getReviewedAt()
        );
    }
}