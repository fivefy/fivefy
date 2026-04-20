package com.fivefy.domain.track.repository;

import com.fivefy.domain.track.entity.Track;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

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

    /**
     * 아티스트별 자유 창작 트랙 목록 조회
     */
    Page<Track> searchArtistFreeCreations(Long ownerUserId, Pageable pageable);

    /**
     * 앨범 수록곡 목록 조회
     */
    List<Track> searchAlbumTracks(Long albumId);
}