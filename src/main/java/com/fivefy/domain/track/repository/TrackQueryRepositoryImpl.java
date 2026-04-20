package com.fivefy.domain.track.repository;

import com.fivefy.domain.track.enums.TrackStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

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
                // 연관 리소스는 optional 구조이므로 left join
                .leftJoin(artist).on(track.artistId.eq(artist.id))
                .leftJoin(album).on(track.albumId.eq(album.id))
                .where(track.id.eq(trackId))
                .fetchOne();
    }

    /**
     * 공개 트랙 목록 조회
     */
    @Override
    public Page<PublicTrackListProjection> searchPublicTracks(Pageable pageable) {

        // 공개된 트랙만 조회 (삭제되지 않았고 PUBLISHED 상태)
        List<PublicTrackListProjection> content = queryFactory
                .select(Projections.constructor(
                        PublicTrackListProjection.class,
                        track.id,
                        track.trackType,
                        track.title,
                        track.artistId,
                        artist.name,
                        track.albumId,
                        album.title,
                        track.durationSec,
                        track.playCount,
                        track.publishedAt
                ))
                .from(track)
                // FREE_CREATION 대응을 위해 left join 유지
                .leftJoin(artist).on(track.artistId.eq(artist.id))
                .leftJoin(album).on(track.albumId.eq(album.id))
                .where(
                        track.deletedAt.isNull(),
                        track.status.eq(TrackStatus.PUBLISHED)
                )
                // 최신 공개순 정렬 (명세 기준)
                .orderBy(track.publishedAt.desc())
                // 페이지네이션 적용
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 개수 조회 (PageResponse 생성용)
        Long total = queryFactory
                .select(track.count())
                .from(track)
                .where(
                        track.deletedAt.isNull(),
                        track.status.eq(TrackStatus.PUBLISHED)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }
}