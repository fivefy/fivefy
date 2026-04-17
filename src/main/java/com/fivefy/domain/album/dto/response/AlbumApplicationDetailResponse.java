package com.fivefy.domain.album.dto.response;

import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.album.entity.AlbumApplication;

import java.time.LocalDateTime;

/**
 * 앨범 등록 신청 상세 응답 DTO
 */
public record AlbumApplicationDetailResponse(
        Long applicationId,
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
    public static AlbumApplicationDetailResponse from(AlbumApplication application) {
        return new AlbumApplicationDetailResponse(
                application.getId(),
                application.getRequesterUserId(),
                application.getArtistId(),
                application.getTitle(),
                application.getDescription(),
                application.getCoverImageUrl(),
                application.getPublishDelayDays(),
                application.getStatus(),
                application.getReviewedByAdminId(),
                application.getReviewedAt(),
                application.getRejectionReason(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }
}