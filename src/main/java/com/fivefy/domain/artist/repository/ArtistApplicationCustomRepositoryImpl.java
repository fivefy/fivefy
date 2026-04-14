package com.fivefy.domain.artist.repository;

import com.fivefy.domain.artist.entity.ArtistApplication;
import com.fivefy.domain.artist.entity.QArtistApplication;
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

    @Override
    public Page<ArtistApplication> searchArtistApplications(Pageable pageable) {
        List<ArtistApplication> contents = queryFactory
                .selectFrom(artistApplication)
                .orderBy(artistApplication.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(artistApplication.count())
                .from(artistApplication)
                .fetchOne();

        return new PageImpl<>(contents, pageable, total == null ? 0L : total);
    }
}
