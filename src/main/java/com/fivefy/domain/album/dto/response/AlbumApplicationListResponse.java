package com.fivefy.domain.album.dto.response;

import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.album.entity.AlbumApplication;

import java.time.LocalDateTime;

/**
 * 앨범 등록 요청 목록 응답 DTO
 */
public record AlbumApplicationListResponse(
        Long requestId,
        Long requesterUserId,
        Long artistId,
        String title,
        ApplicationStatus status,
        LocalDateTime createdAt
) {
    public static AlbumApplicationListResponse from(AlbumApplication request) {
        return new AlbumApplicationListResponse(
                request.getId(),
                request.getRequesterUserId(),
                request.getArtistId(),
                request.getTitle(),
                request.getStatus(),
                request.getCreatedAt()
        );
    }
}