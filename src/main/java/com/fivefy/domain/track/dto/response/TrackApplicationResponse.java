package com.fivefy.domain.track.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.track.entity.TrackApplication;
import com.fivefy.domain.track.enums.TrackType;

import java.time.LocalDateTime;

/**
 * 트랙 등록 신청 생성 응답 DTO
 *
 * - 자유 창작 / 정식 발매 공통으로 사용
 * - 자유 창작의 경우 artistId, albumId는 null
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TrackApplicationResponse(

        Long applicationId,
        TrackType trackType,
        Long artistId,
        Long albumId,
        String title,
        ApplicationStatus status,
        LocalDateTime createdAt
) {

    /**
     * TrackApplication 엔티티 → 응답 DTO 변환
     */
    public static TrackApplicationResponse from(TrackApplication trackApplication) {
        return new TrackApplicationResponse(
                trackApplication.getId(),
                trackApplication.getTrackType(),
                trackApplication.getArtistId(),
                trackApplication.getAlbumId(),
                trackApplication.getTitle(),
                trackApplication.getStatus(),
                trackApplication.getCreatedAt()
        );
    }
}