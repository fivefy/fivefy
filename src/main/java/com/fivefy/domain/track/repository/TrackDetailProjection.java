package com.fivefy.domain.track.repository;

/**
 * 트랙 상세 조회 보강 데이터
 */
public record TrackDetailProjection(
        String artistName,
        String albumTitle
) {
}