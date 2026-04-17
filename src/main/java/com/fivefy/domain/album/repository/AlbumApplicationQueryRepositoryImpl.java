package com.fivefy.domain.album.repository;

import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.album.entity.AlbumApplication;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Objects;

import static com.fivefy.domain.album.entity.QAlbumApplication.albumApplication;

@RequiredArgsConstructor
public class AlbumApplicationQueryRepositoryImpl implements AlbumApplicationQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public boolean existsPendingApplication(Long requesterUserId, Long artistId, String title) {
        Integer result = queryFactory
                .selectOne()
                .from(albumApplication)
                .where(
                        albumApplication.requesterUserId.eq(requesterUserId),
                        albumApplication.artistId.eq(artistId),
                        albumApplication.title.eq(title),
                        albumApplication.status.eq(ApplicationStatus.PENDING)
                )
                .fetchFirst();

        return result != null;
    }

    @Override
    public List<AlbumApplication> searchMyAlbumApplications(Long requesterUserId) {
        return queryFactory
                .selectFrom(albumApplication)
                .where(albumApplication.requesterUserId.eq(requesterUserId))
                .orderBy(albumApplication.createdAt.desc())
                .fetch();
    }

    @Override
    public Page<AlbumApplication> searchAlbumApplications(
            ApplicationStatus status,
            Pageable pageable
    ) {
        List<AlbumApplication> content = queryFactory
                .selectFrom(albumApplication)
                .where(statusEq(status))
                .orderBy(albumApplication.createdAt.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(albumApplication.count())
                .from(albumApplication)
                .where(statusEq(status))
                .fetchOne();

        return new PageImpl<>(content, pageable, Objects.requireNonNullElse(total, 0L));
    }

    // 상태 필터 조건
    private BooleanExpression statusEq(ApplicationStatus status) {
        return status != null ? albumApplication.status.eq(status) : null;
    }
}