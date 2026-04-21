package com.fivefy.domain.track.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fivefy.domain.track.enums.TrackType;

import java.time.LocalDateTime;

/**
 * 공개 트랙 목록 조회 응답 DTO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PublicTrackListResponse(
        Long trackId,
        TrackType trackType,
        String title,
        Long artistId,
        String artistName,
        Long albumId,
        String albumTitle,
        Long durationSec,
        Long playCount,
        LocalDateTime publishedAt
) {
    public static PublicTrackListResponse of(
            Long trackId,
            TrackType trackType,
            String title,
            Long artistId,
            String artistName,
            Long albumId,
            String albumTitle,
            Long durationSec,
            Long playCount,
            LocalDateTime publishedAt
    ) {
        return new PublicTrackListResponse(
                trackId,
                trackType,
                title,
                artistId,
                artistName,
                albumId,
                albumTitle,
                durationSec,
                playCount,
                publishedAt
        );
    }
}