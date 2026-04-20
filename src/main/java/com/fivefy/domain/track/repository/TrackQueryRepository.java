package com.fivefy.domain.track.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Track Querydsl 전용 Repository
 */
public interface TrackQueryRepository {

    /**
     * 트랙 상세 조회 보강 데이터 조회
     */
    TrackDetailProjection findTrackDetailById(Long trackId);

    /**
     * 공개 트랙 목록 조회
     */
    Page<PublicTrackListProjection> searchPublicTracks(Pageable pageable);
}