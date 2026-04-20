package com.fivefy.domain.track.repository;

/**
 * Track Querydsl 전용 Repository
 */
public interface TrackQueryRepository {

    TrackDetailProjection findTrackDetailById(Long trackId);
}