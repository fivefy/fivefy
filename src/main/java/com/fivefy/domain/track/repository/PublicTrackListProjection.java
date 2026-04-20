package com.fivefy.domain.track.repository;

import com.fivefy.domain.track.enums.TrackType;

import java.time.LocalDateTime;

/**
 * 공개 트랙 목록 조회 Projection
 */
public record PublicTrackListProjection(
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
}