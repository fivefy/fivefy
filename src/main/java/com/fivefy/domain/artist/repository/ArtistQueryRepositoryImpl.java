package com.fivefy.domain.artist.repository;

import com.fivefy.domain.artist.entity.Artist;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.fivefy.domain.artist.entity.QArtist.artist;

/**
 * Artist Querydsl 구현체
 */
@Repository
@RequiredArgsConstructor
public class ArtistQueryRepositoryImpl implements ArtistQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 내 아티스트 목록 조회
     */
    @Override
    public List<Artist> findMyArtists(Long ownerUserId) {
        // 소유자 기준으로 삭제되지 않은 아티스트를 최신순으로 조회한다.
        return queryFactory
                .selectFrom(artist)
                .where(
                        artist.ownerUserId.eq(ownerUserId),
                        artist.deletedAt.isNull()
                )
                .orderBy(artist.createdAt.desc())
                .fetch();
    }
}