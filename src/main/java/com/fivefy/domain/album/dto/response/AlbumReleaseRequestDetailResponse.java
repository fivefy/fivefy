package com.fivefy.domain.album.dto.response;

import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.album.entity.AlbumReleaseRequest;

import java.time.LocalDateTime;

/**
 * 앨범 등록 요청 상세 응답 DTO
 */
public record AlbumReleaseRequestDetailResponse(
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
    public static AlbumReleaseRequestDetailResponse from(AlbumReleaseRequest request) {
        return new AlbumReleaseRequestDetailResponse(
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