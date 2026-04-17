package com.fivefy.domain.album.dto.response;

import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.album.entity.AlbumApplication;

import java.time.LocalDateTime;

/**
 * 앨범 등록 요청 상세 응답 DTO
 */
public record AlbumApplicationDetailResponse(
        Long requestId,
        Long requesterUserId,
        Long artistId,
        String title,
        String description,
        String coverImageUrl,
        Integer publishDelayDays,
        ApplicationStatus status,
        Long reviewedByAdminId,
        LocalDateTime reviewedAt,
        String rejectionReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AlbumApplicationDetailResponse from(AlbumApplication request) {
        return new AlbumApplicationDetailResponse(
                request.getId(),
                request.getRequesterUserId(),
                request.getArtistId(),
                request.getTitle(),
                request.getDescription(),
                request.getCoverImageUrl(),
                request.getPublishDelayDays(),
                request.getStatus(),
                request.getReviewedByAdminId(),
                request.getReviewedAt(),
                request.getRejectionReason(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}