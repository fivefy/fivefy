package com.fivefy.domain.track.repository;

import com.fivefy.domain.track.entity.TrackComment;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.fivefy.domain.track.entity.QTrackComment.trackComment;

/**
 * TrackComment Querydsl 구현체
 */
@Repository
@RequiredArgsConstructor
public class TrackCommentQueryRepositoryImpl implements TrackCommentQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 트랙 댓글 목록 조회
     */
    @Override
    public Page<TrackComment> getTrackComments(Long trackId, Pageable pageable) {

        // 특정 트랙의 삭제되지 않은 댓글을 최신순으로 조회
        List<TrackComment> content = queryFactory
                .selectFrom(trackComment)
                .where(
                        trackComment.trackId.eq(trackId),
                        trackComment.deletedAt.isNull()
                )
                // 최신순 정렬
                .orderBy(trackComment.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 개수 조회 (PageResponse 생성용)
        Long total = queryFactory
                .select(trackComment.count())
                .from(trackComment)
                .where(
                        trackComment.trackId.eq(trackId),
                        trackComment.deletedAt.isNull()
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }
}