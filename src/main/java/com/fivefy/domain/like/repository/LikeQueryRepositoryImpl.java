package com.fivefy.domain.like.repository;

import com.fivefy.domain.album.entity.QAlbum;
import com.fivefy.domain.artist.entity.QArtist;
import com.fivefy.domain.like.dto.response.LikeGetResponse;
import com.fivefy.domain.like.entity.QLike;
import com.fivefy.domain.like.enums.TargetType;
import com.fivefy.domain.track.entity.QTrack;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RequiredArgsConstructor
public class LikeQueryRepositoryImpl implements LikeQueryRepository {

    private final JPAQueryFactory queryFactory;

    private final QLike like = QLike.like;
    private final QTrack track = QTrack.track;
    private final QAlbum album = QAlbum.album;
    private final QArtist artist = QArtist.artist;

    @Override
    public Page<LikeGetResponse> findLikesWithTarget(
            Long userId, TargetType targetType, Pageable pageable) {
        BooleanExpression condition = like.userId.eq(userId);

        if (targetType != null) {
            condition = condition.and(like.targetType.eq(targetType));
        }

        List<LikeGetResponse> results = queryFactory
                .select(Projections.constructor(LikeGetResponse.class,
                        like.id,
                        like.targetId,
                        like.targetType,
                        track.title.coalesce(album.title),
                        artist.name,
                        like.createdAt
                ))
                .from(like)
                .leftJoin(track).on(track.id.eq(like.targetId)
                        .and(like.targetType.eq(TargetType.TRACK)))
                .leftJoin(album).on(album.id.eq(like.targetId)
                        .and(like.targetType.eq(TargetType.ALBUM)))
                .leftJoin(artist).on(artist.id.eq(track.artistId.coalesce(album.artistId)))
                .where(condition)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(like.count())
                .from(like)
                .where(condition)
                .fetchOne();

        return new PageImpl<>(results, pageable, total);
    }
}
