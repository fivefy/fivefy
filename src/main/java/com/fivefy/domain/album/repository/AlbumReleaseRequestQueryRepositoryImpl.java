package com.fivefy.domain.album.repository;

import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.album.entity.AlbumReleaseRequest;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Objects;

import static com.fivefy.domain.album.entity.QAlbumReleaseRequest.albumReleaseRequest;

@RequiredArgsConstructor
public class AlbumReleaseRequestQueryRepositoryImpl implements AlbumReleaseRequestQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public boolean existsPendingRequest(Long requesterUserId, Long artistId, String title) {
        Integer result = queryFactory
                .selectOne()
                .from(albumReleaseRequest)
                .where(
                        albumReleaseRequest.requesterUserId.eq(requesterUserId),
                        albumReleaseRequest.artistId.eq(artistId),
                        albumReleaseRequest.title.eq(title),
                        albumReleaseRequest.status.eq(ApplicationStatus.PENDING)
                )
                .fetchFirst();

        return result != null;
    }

    @Override
    public List<AlbumReleaseRequest> searchMyAlbumReleaseRequests(Long requesterUserId) {
        return queryFactory
                .selectFrom(albumReleaseRequest)
                .where(albumReleaseRequest.requesterUserId.eq(requesterUserId))
                .orderBy(albumReleaseRequest.createdAt.desc())
                .fetch();
    }

    @Override
    public Page<AlbumReleaseRequest> searchAlbumReleaseRequests(
            ApplicationStatus status,
            Pageable pageable
    ) {
        List<AlbumReleaseRequest> content = queryFactory
                .selectFrom(albumReleaseRequest)
                .where(statusEq(status))
                .orderBy(albumReleaseRequest.createdAt.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(albumReleaseRequest.count())
                .from(albumReleaseRequest)
                .where(statusEq(status))
                .fetchOne();

        return new PageImpl<>(content, pageable, Objects.requireNonNullElse(total, 0L));
    }

    // 상태 필터 조건
    private BooleanExpression statusEq(ApplicationStatus status) {
        return status != null ? albumReleaseRequest.status.eq(status) : null;
    }
}