package com.fivefy.domain.artist.repository;

import com.fivefy.domain.artist.entity.Artist;

import java.util.List;

/**
 * Artist Querydsl 전용 Repository
 */
public interface ArtistQueryRepository {
    /**
     * 내 아티스트 목록 조회
     */
    List<Artist> findMyArtists(Long ownerUserId);
}
