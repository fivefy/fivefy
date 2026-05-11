package com.fivefy.domain.track.repository;

import com.fivefy.domain.track.entity.Track;
import com.fivefy.domain.track.enums.TrackStatus;
import com.fivefy.domain.track.enums.TrackType;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
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
    public Slice<PublicTrackListProjection> searchPublicTracks(Pageable pageable) {

        int pageSize = pageable.getPageSize();

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
                        track.status.eq(TrackStatus.PUBLISHED),
                        track.trackType.eq(TrackType.FREE_CREATION)
                                .or(
                                        track.trackType.eq(TrackType.OFFICIAL_RELEASE)
                                                .and(album.deletedAt.isNull())
                                                .and(album.status.eq(com.fivefy.domain.album.enums.AlbumStatus.PUBLISHED))
                                                .and(artist.deletedAt.isNull())
                                )
                )
                // 최신 공개순 정렬 (명세 기준)
                .orderBy(track.publishedAt.desc())
                // Slice 응답을 위해 다음 페이지 존재 여부 확인용으로 1건 더 조회
                .offset(pageable.getOffset())
                .limit(pageSize + 1)
                .fetch();

        boolean hasNext = content.size() > pageSize;

        if (hasNext) {
            content = content.subList(0, pageSize);
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }

    /**
     * 앨범 수록곡 목록 조회
     */
    @Override
    public List<Track> searchAlbumTracks(Long albumId) {

        // 해당 앨범에 속한 공개 정식 발매 트랙만 조회
        return queryFactory
                .selectFrom(track)
                .where(
                        track.albumId.eq(albumId),
                        track.trackType.eq(TrackType.OFFICIAL_RELEASE),
                        track.status.eq(TrackStatus.PUBLISHED),
                        track.deletedAt.isNull()
                )
                // 앨범 수록 순서대로 정렬
                .orderBy(track.trackNumber.asc())
                .fetch();
    }
}