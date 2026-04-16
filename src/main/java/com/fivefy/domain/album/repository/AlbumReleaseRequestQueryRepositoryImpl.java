package com.fivefy.domain.album.repository;

import com.fivefy.common.enums.ApplicationStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

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
}