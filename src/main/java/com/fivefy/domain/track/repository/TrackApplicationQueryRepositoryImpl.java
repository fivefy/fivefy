package com.fivefy.domain.track.repository;

import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.track.entity.TrackApplication;
import com.fivefy.domain.track.enums.TrackType;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.fivefy.domain.track.entity.QTrackApplication.trackApplication;

/**
 * TrackApplication Querydsl 구현체
 */
@Repository
@RequiredArgsConstructor
public class TrackApplicationQueryRepositoryImpl implements TrackApplicationQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 자유 창작 PENDING 중복 신청 여부 조회
     */
    @Override
    public boolean existsPendingFreeCreationApplication(
            Long requesterUserId,
            String title,
            String audioUrl
    ) {
        Integer result = queryFactory
                .selectOne()
                .from(trackApplication)
                .where(
                        trackApplication.requesterUserId.eq(requesterUserId),
                        trackApplication.status.eq(ApplicationStatus.PENDING),
                        trackApplication.artistId.isNull(),
                        trackApplication.albumId.isNull(),
                        trackApplication.trackNumber.isNull(),
                        trackApplication.title.eq(title),
                        trackApplication.audioUrl.eq(audioUrl)
                )
                .fetchFirst();

        return result != null;
    }

    /**
     * 정식 발매 PENDING 중복 신청 여부 조회
     */
    @Override
    public boolean existsPendingOfficialReleaseApplication(
            Long requesterUserId,
            Long artistId,
            Long albumId,
            Long trackNumber,
            String title
    ) {
        Integer result = queryFactory
                .selectOne()
                .from(trackApplication)
                .where(
                        trackApplication.requesterUserId.eq(requesterUserId),
                        trackApplication.status.eq(ApplicationStatus.PENDING),
                        trackApplication.trackType.eq(TrackType.OFFICIAL_RELEASE),
                        trackApplication.artistId.eq(artistId),
                        trackApplication.albumId.eq(albumId),
                        officialReleaseDuplicateCondition(trackNumber, title)
                )
                .fetchFirst();

        return result != null;
    }

    /**
     * 내 트랙 등록 신청 목록 조회
     */
    @Override
    public List<TrackApplication> searchMyTrackApplications(Long requesterUserId) {
        return queryFactory
                .selectFrom(trackApplication)
                .where(trackApplication.requesterUserId.eq(requesterUserId))
                .orderBy(trackApplication.createdAt.desc())
                .fetch();
    }

    @Override
    public Page<TrackApplication> searchTrackApplications(ApplicationStatus status, Pageable pageable) {
        return null;
    }

    // 같은 앨범 내 동일 trackNumber 또는 동일 title 중복 조건
    private BooleanExpression officialReleaseDuplicateCondition(
            Long trackNumber,
            String title
    ) {
        BooleanExpression sameTrackNumber = trackApplication.trackNumber.eq(trackNumber);
        BooleanExpression sameTitle = trackApplication.title.eq(title);

        return sameTrackNumber.or(sameTitle);
    }
}
