package com.fivefy.domain.track.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import static com.fivefy.domain.album.entity.QAlbum.album;
import static com.fivefy.domain.artist.entity.QArtist.artist;
import static com.fivefy.domain.track.entity.QTrack.track;

/**
 * Track Querydsl 구현체
 */
@Repository
@RequiredArgsConstructor
public class TrackQueryRepositoryImpl implements TrackQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 트랙 상세 조회 보강 데이터 조회
     */
    @Override
    public TrackDetailProjection findTrackDetailById(Long trackId) {
        return queryFactory
                .select(Projections.constructor(
                        TrackDetailProjection.class,
                        artist.name,
                        album.title
                ))
                .from(track)
                .leftJoin(artist).on(track.artistId.eq(artist.id))
                .leftJoin(album).on(track.albumId.eq(album.id))
                .where(track.id.eq(trackId))
                .fetchOne();
    }
}