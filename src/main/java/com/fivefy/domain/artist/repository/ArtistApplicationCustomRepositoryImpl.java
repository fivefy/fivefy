package com.fivefy.domain.artist.repository;

import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.artist.entity.ArtistApplication;
import com.fivefy.domain.artist.entity.QArtistApplication;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 아티스트 등록 요청 Querydsl Repository 구현체
 */
@RequiredArgsConstructor
public class ArtistApplicationCustomRepositoryImpl implements ArtistApplicationCustomRepository {

    private final JPAQueryFactory queryFactory;

    private static final QArtistApplication artistApplication = QArtistApplication.artistApplication;

    /**
     * 관리자용 아티스트 등록 요청 목록을 페이징 조회한다.
     */
    @Override
    public Page<ArtistApplication> searchArtistApplications(ApplicationStatus status, Pageable pageable) {
        // 페이징 조건과 정렬 조건을 반영해 등록 요청 목록을 조회한다.
        List<ArtistApplication> contents = queryFactory
                .selectFrom(artistApplication)
                .where(statusEq(status))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(getOrderSpecifiers(pageable))
                .fetch();

        // 전체 등록 요청 수를 조회한다.
        Long total = queryFactory
                .select(artistApplication.count())
                .from(artistApplication)
                .where(statusEq(status))
                .fetchOne();

        // 조회 결과를 Page 객체로 변환해 반환한다.
        return new PageImpl<>(contents, pageable, total == null ? 0L : total);
    }

    /**
     * Pageable의 정렬 조건을 Querydsl OrderSpecifier로 변환한다.
     */
    private OrderSpecifier<?>[] getOrderSpecifiers(Pageable pageable) {

        QArtistApplication artistApplication = QArtistApplication.artistApplication;

        return pageable.getSort().stream()
                .map(order -> {
                    // Spring Sort 방향을 Querydsl 정렬 방향으로 변환한다.
                    Order direction = order.isAscending() ? Order.ASC : Order.DESC;

                    // 정렬 대상 필드에 맞는 OrderSpecifier를 생성한다.
                    return switch (order.getProperty()) {
                        case "createdAt" -> new OrderSpecifier<>(direction, artistApplication.createdAt);
                        case "status" -> new OrderSpecifier<>(direction, artistApplication.status);
                        // 지원하지 않는 정렬 조건이면 createdAt 기준 최신/오래된 순 정렬로 fallback 한다.
                        default -> new OrderSpecifier<>(Order.ASC, artistApplication.createdAt);
                    };
                })
                .toArray(OrderSpecifier[]::new);
    }

    /**
     * 상태 조건이 있으면 해당 상태만 조회한다.
     */
    private BooleanExpression statusEq(ApplicationStatus status) {
        return status == null ? null : artistApplication.status.eq(status);
    }
}
