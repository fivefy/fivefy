package com.fivefy.domain.search.repository;

import com.fivefy.domain.album.entity.Album;
import com.fivefy.domain.album.entity.QAlbum;
import com.fivefy.domain.album.enums.AlbumStatus;
import com.fivefy.domain.artist.entity.Artist;
import com.fivefy.domain.artist.entity.QArtist;
import com.fivefy.domain.artist.enums.ArtistStatus;
import com.fivefy.domain.track.entity.QTrack;
import com.fivefy.domain.track.entity.Track;
import com.fivefy.domain.track.enums.TrackStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SearchQueryRepositoryImpl implements SearchQueryRepository {

    private final JPAQueryFactory queryFactory;

    private final QArtist artist = QArtist.artist;
    private final QTrack track = QTrack.track;
    private final QAlbum album = QAlbum.album;

    @Override
    public List<Artist> searchArtists(String keyword) {
        return queryFactory
                .selectFrom(artist)
                .where(
                        artistKeyword(keyword),
                        artist.deletedAt.isNull(),
                        artist.status.eq(ArtistStatus.ACTIVE)
                )
                .fetch();
    }

    @Override
    public List<Track> searchTracks(String keyword) {
        List<Long> artistIds = getArtistIdsByKeyword(keyword);

        return queryFactory
                .selectFrom(track)
                .where(
                        trackKeyword(keyword)
                                .or(artistIds.isEmpty() ? null : track.artistId.in(artistIds)),
                        track.deletedAt.isNull(),
                        track.status.eq(TrackStatus.PUBLISHED)
                )
                .fetch();
    }

    @Override
    public List<Album> searchAlbums(String keyword) {
        List<Long> artistIds = getArtistIdsByKeyword(keyword);

        return queryFactory
                .selectFrom(album)
                .where(
                        albumKeyword(keyword)
                                .or(artistIds.isEmpty() ? null : album.artistId.in(artistIds)),
                        album.deletedAt.isNull(),
                        album.status.eq(AlbumStatus.PUBLISHED)
                )
                .fetch();
    }

    // 아티스트 ID 조회 헬퍼
    private List<Long> getArtistIdsByKeyword(String keyword) {
        return queryFactory
                .select(artist.id)
                .from(artist)
                .where(
                        artistKeyword(keyword),
                        artist.deletedAt.isNull(),
                        artist.status.eq(ArtistStatus.ACTIVE)
                )
                .fetch();
    }

    // 동적 조건 헬퍼 메서드
    private BooleanExpression artistKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        return artist.name.containsIgnoreCase(keyword);
    }

    private BooleanExpression trackKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        return track.title.containsIgnoreCase(keyword);
    }

    private BooleanExpression albumKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        return album.title.containsIgnoreCase(keyword);
    }
}