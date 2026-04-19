package com.fivefy.domain.track.repository;

import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.track.entity.QTrackApplication;
import com.fivefy.domain.track.entity.TrackApplication;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

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
                .from(QTrackApplication.trackApplication)
                .where(
                        QTrackApplication.trackApplication.requesterUserId.eq(requesterUserId),
                        QTrackApplication.trackApplication.status.eq(ApplicationStatus.PENDING),
                        QTrackApplication.trackApplication.artistId.isNull(),
                        QTrackApplication.trackApplication.albumId.isNull(),
                        QTrackApplication.trackApplication.trackNumber.isNull(),
                        QTrackApplication.trackApplication.title.eq(title),
                        QTrackApplication.trackApplication.audioUrl.eq(audioUrl)
                )
                .fetchFirst();

        return result != null;
    }

    @Override
    public boolean existsPendingOfficialReleaseApplication(Long requesterUserId, Long artistId, Long albumId, Long trackNumber, String title) {
        return false;
    }

    @Override
    public List<TrackApplication> searchMyTrackApplications(Long requesterUserId) {
        return List.of();
    }

    @Override
    public Page<TrackApplication> searchTrackApplications(ApplicationStatus status, Pageable pageable) {
        return null;
    }
}
