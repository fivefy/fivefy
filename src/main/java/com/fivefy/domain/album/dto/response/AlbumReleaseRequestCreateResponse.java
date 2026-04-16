package com.fivefy.domain.album.dto.response;

import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.album.entity.AlbumReleaseRequest;

import java.time.LocalDateTime;

/**
 * 앨범 등록 요청 생성 응답 DTO
 */
public record AlbumReleaseRequestCreateResponse(

        Long requestId,
        Long artistId,
        String title,
        ApplicationStatus status,
        LocalDateTime createdAt

) {
    public static AlbumReleaseRequestCreateResponse from(AlbumReleaseRequest albumReleaseRequest) {
        return new AlbumReleaseRequestCreateResponse(
                albumReleaseRequest.getId(),
                albumReleaseRequest.getArtistId(),
                albumReleaseRequest.getTitle(),
                albumReleaseRequest.getStatus(),
                albumReleaseRequest.getCreatedAt()
        );
    }
}