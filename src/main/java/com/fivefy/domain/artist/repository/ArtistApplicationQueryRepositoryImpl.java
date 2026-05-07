package com.fivefy.domain.artist.repository;

import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.artist.entity.ArtistApplication;
import com.fivefy.domain.artist.enums.ArtistType;
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

import static com.fivefy.domain.artist.entity.QArtistApplication.artistApplication;

/**
 * 아티스트 등록 신청 Querydsl Repository 구현체
 */
@RequiredArgsConstructor
public class ArtistApplicationQueryRepositoryImpl implements ArtistApplicationQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 진행 중인 동일 아티스트 신청 존재 여부 조회
     */
    @Override
    public boolean existsPendingApplication(Long requesterUserId, String requestedName, ArtistType artistType) {
        // PENDING 상태의 동일 아티스트 신청 존재 여부 확인
        Integer result = queryFactory
                .selectOne()
                .from(artistApplication)
                .where(
                        artistApplication.requesterUserId.eq(requesterUserId),
                        artistApplication.requestedName.eq(requestedName),
                        artistApplication.artistType.eq(artistType),
                        artistApplication.status.eq(ApplicationStatus.PENDING)
                )
                .fetchFirst();

        return result != null;
    }

    /**
     * 승인된 동일 아티스트 신청 존재 여부 조회
     */
    @Override
    public boolean existsApprovedApplication(Long requesterUserId, String requestedName, ArtistType artistType) {
        // APPROVED 상태의 동일 아티스트 신청 존재 여부 확인
        Integer result = queryFactory
                .selectOne()
                .from(artistApplication)
                .where(
                        artistApplication.requesterUserId.eq(requesterUserId),
                        artistApplication.requestedName.eq(requestedName),
                        artistApplication.artistType.eq(artistType),
                        artistApplication.status.eq(ApplicationStatus.APPROVED)
                )
                .fetchFirst();

        return result != null;
    }

    /**
     * 사용자 본인의 아티스트 등록 신청 목록 조회 (최신순)
     */
    @Override
    public List<ArtistApplication> searchMyArtistApplications(Long requesterUserId) {
        // 사용자 기준 신청 목록을 최신순으로 조회
        return queryFactory
                .selectFrom(artistApplication)
                .where(artistApplication.requesterUserId.eq(requesterUserId))
                .orderBy(artistApplication.createdAt.desc())
                .fetch();
    }

    /**
     * 아티스트 등록 신청 목록 페이징 조회
     */
    @Override
    public Page<ArtistApplication> searchArtistApplications(ApplicationStatus status, Pageable pageable) {
        // 페이징 조건과 정렬 조건을 반영한 실제 데이터 조회
        List<ArtistApplication> contents = queryFactory
                .selectFrom(artistApplication)
                .where(statusEq(status))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(getOrderSpecifiers(pageable))
                .fetch();

        // 전체 등록 신청 수 조회
        Long total = queryFactory
                .select(artistApplication.count())
                .from(artistApplication)
                .where(statusEq(status))
                .fetchOne();

        // 조회 결과를 Page 객체로 변환하여 반환
        return new PageImpl<>(contents, pageable, Objects.requireNonNullElse(total, 0L));
    }

    /**
     * Pageable 정렬 조건을 Querydsl OrderSpecifier로 변환
     */
    private OrderSpecifier<?>[] getOrderSpecifiers(Pageable pageable) {

        return pageable.getSort().stream()
                .map(order -> {
                    // Spring Sort -> Querydsl 정렬 방향으로 변환
                    Order direction = order.isAscending() ? Order.ASC : Order.DESC;

                    // 정렬 대상 필드에 맞는 OrderSpecifier를 생성
                    return switch (order.getProperty()) {
                        case "createdAt" -> new OrderSpecifier<>(direction, artistApplication.createdAt);
                        case "status" -> new OrderSpecifier<>(direction, artistApplication.status);
                        // 지원하지 않는 정렬 조건이면 createdAt 기준 최신/오래된 순 정렬로 fallback
                        default -> new OrderSpecifier<>(Order.ASC, artistApplication.createdAt);
                    };
                })
                .toArray(OrderSpecifier[]::new);
    }

    // 상태 필터 조건
    private BooleanExpression statusEq(ApplicationStatus status) {
        // 상태가 없으면 전체 조회
        return status != null ? artistApplication.status.eq(status) : null;
    }
}