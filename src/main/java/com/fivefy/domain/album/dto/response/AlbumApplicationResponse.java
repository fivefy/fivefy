package com.fivefy.domain.album.dto.response;

import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.album.entity.AlbumApplication;

import java.time.LocalDateTime;

/**
 * 앨범 등록 요청 생성 응답 및 내 앨범 등록 요청 목록 응답 DTO
 */
public record AlbumApplicationResponse(

        Long requestId,
        Long artistId,
        String title,
        ApplicationStatus status,
        LocalDateTime createdAt

) {
    public static AlbumApplicationResponse from(AlbumApplication albumApplication) {
        return new AlbumApplicationResponse(
                albumApplication.getId(),
                albumApplication.getArtistId(),
                albumApplication.getTitle(),
                albumApplication.getStatus(),
                albumApplication.getCreatedAt()
        );
    }
}