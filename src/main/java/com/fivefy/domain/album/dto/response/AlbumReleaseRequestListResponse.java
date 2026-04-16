package com.fivefy.domain.album.dto.response;

import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.album.entity.AlbumReleaseRequest;

import java.time.LocalDateTime;

/**
 * 앨범 등록 요청 목록 응답 DTO
 */
public record AlbumReleaseRequestListResponse(
        Long requestId,
        Long requesterUserId,
        Long artistId,
        String title,
        ApplicationStatus status,
        LocalDateTime createdAt
) {
    public static AlbumReleaseRequestListResponse from(AlbumReleaseRequest request) {
        return new AlbumReleaseRequestListResponse(
                request.getId(),
                request.getRequesterUserId(),
                request.getArtistId(),
                request.getTitle(),
                request.getStatus(),
                request.getCreatedAt()
        );
    }
}