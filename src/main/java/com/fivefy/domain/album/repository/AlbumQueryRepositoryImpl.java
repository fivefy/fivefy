package com.fivefy.domain.album.repository;

import com.fivefy.domain.album.entity.Album;
import com.fivefy.domain.album.enums.AlbumStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.fivefy.domain.album.entity.QAlbum.album;

@RequiredArgsConstructor
public class AlbumQueryRepositoryImpl implements AlbumQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Album> searchArtistAlbums(Long artistId) {
        return queryFactory
                .selectFrom(album)
                .where(
                        album.artistId.eq(artistId),
                        album.status.eq(AlbumStatus.PUBLISHED),
                        album.deletedAt.isNull()
                )
                .orderBy(album.publishedAt.desc())
                .fetch();
    }
}