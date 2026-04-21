package com.fivefy.domain.track.dto.response;

import com.fivefy.domain.track.entity.Track;

import java.time.LocalDateTime;

/**
 * 아티스트별 자유 창작 트랙 목록 조회 응답 DTO
 */
public record ArtistFreeCreationTrackResponse(
        Long trackId,
        String title,
        Long durationSec,
        LocalDateTime publishedAt
) {

    public static ArtistFreeCreationTrackResponse from(Track track) {
        return new ArtistFreeCreationTrackResponse(
                track.getId(),
                track.getTitle(),
                track.getDurationSec(),
                track.getPublishedAt()
        );
    }
}