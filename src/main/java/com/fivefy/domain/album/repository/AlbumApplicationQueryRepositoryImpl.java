package com.fivefy.domain.album.repository;

import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.album.entity.AlbumApplication;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Objects;

import static com.fivefy.domain.album.entity.QAlbumApplication.albumApplication;
/**
 * 앨범 등록 신청 Querydsl 조회 구현체
 */
@RequiredArgsConstructor
public class AlbumApplicationQueryRepositoryImpl implements AlbumApplicationQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 진행 중인 동일 앨범 신청 존재 여부 조회
     */
    @Override
    public boolean existsPendingApplication(Long requesterUserId, Long artistId, String title) {
        // PENDING 상태의 상태의 동일 앨범 신청 존재 여부 확인
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

    /**
     * 승인된 동일 앨범 신청 존재 여부 조회
     */
    @Override
    public boolean existsApprovedApplication(Long requesterUserId, Long artistId, String title) {
        // APPROVED 상태의 동일 앨범 신청 존재 여부 확인
        Integer result = queryFactory
                .selectOne()
                .from(albumApplication)
                .where(
                        albumApplication.requesterUserId.eq(requesterUserId),
                        albumApplication.artistId.eq(artistId),
                        albumApplication.title.eq(title),
                        albumApplication.status.eq(ApplicationStatus.APPROVED)
                )
                .fetchFirst();

        return result != null;
    }

    /**
     * 사용자 본인의 앨범 등록 신청 목록 조회 (최신순)
     */
    @Override
    public List<AlbumApplication> searchMyAlbumApplications(Long requesterUserId) {
        // 사용자 기준 신청 목록을 최신순으로 조회
        return queryFactory
                .selectFrom(albumApplication)
                .where(albumApplication.requesterUserId.eq(requesterUserId))
                .orderBy(albumApplication.createdAt.desc())
                .fetch();
    }

    /**
     * 앨범 등록 신청 목록 페이징 조회
     */
    @Override
    public Page<AlbumApplication> searchAlbumApplications(
            ApplicationStatus status,
            Pageable pageable
    ) {
        // 페이징 조건과 정렬 조건을 반영한 실제 데이터 조회
        List<AlbumApplication> content = queryFactory
                .selectFrom(albumApplication)
                .where(statusEq(status))
                .orderBy(getOrderSpecifiers(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 등록 신청 수 조회
        Long total = queryFactory
                .select(albumApplication.count())
                .from(albumApplication)
                .where(statusEq(status))
                .fetchOne();

        // 조회 결과를 Page 객체로 변환하여 반환
        return new PageImpl<>(content, pageable, Objects.requireNonNullElse(total, 0L));
    }

    /**
     * Pageable 정렬 조건을 Querydsl OrderSpecifier로 변환
     */
    private OrderSpecifier<?>[] getOrderSpecifiers(Pageable pageable) {
        return pageable.getSort().stream()
                .map(order -> {
                    // Spring Sort -> Querydsl 정렬 방향으로 변환
                    Order direction = order.isAscending() ? Order.ASC : Order.DESC;

                    // 정렬 대상 필드에 맞는 OrderSpecifier 생성
                    return switch (order.getProperty()) {
                        case "createdAt" -> new OrderSpecifier<>(direction, albumApplication.createdAt);
                        case "status" -> new OrderSpecifier<>(direction, albumApplication.status);
                        // 지원하지 않는 정렬 조건이면 createdAt 기준으로 fallback
                        default -> new OrderSpecifier<>(Order.DESC, albumApplication.createdAt);
                    };
                })
                .toArray(OrderSpecifier[]::new);
    }

    // 상태 필터 조건
    private BooleanExpression statusEq(ApplicationStatus status) {
        // 상태가 없으면 전체 조회
        return status != null ? albumApplication.status.eq(status) : null;
    }
}