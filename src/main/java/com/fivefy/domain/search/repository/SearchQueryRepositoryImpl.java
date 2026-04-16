package com.fivefy.domain.search.repository;

import com.fivefy.domain.album.entity.Album;
import com.fivefy.domain.artist.entity.Artist;
import com.fivefy.domain.track.entity.Track;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SearchQueryRepositoryImpl implements SearchQueryRepository {

    private final EntityManager em;

    @Override
    @SuppressWarnings("unchecked")
    public List<Artist> searchArtists(String keyword, int limit) {
        return em.createNativeQuery(
                        "SELECT * FROM artists WHERE MATCH(name) AGAINST(:keyword IN BOOLEAN MODE)" +
                                " AND deleted_at IS NULL AND status = 'ACTIVE' LIMIT :limit",
                        Artist.class)
                .setParameter("keyword", keyword)
                .setParameter("limit", limit)
                .getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page<Track> searchTracks(String keyword, List<Long> artistIds, Pageable pageable) {
        List<Long> ids = artistIds.isEmpty() ? List.of(-1L) : artistIds;

        List<Track> results = em.createNativeQuery(
                        "SELECT * FROM tracks WHERE (" +
                                "MATCH(title) AGAINST(:keyword IN BOOLEAN MODE) OR artist_id IN (:ids))" +
                                " AND deleted_at IS NULL AND status = 'PUBLISHED' LIMIT :offset, :size",
                        Track.class)
                .setParameter("keyword", keyword)
                .setParameter("ids", ids)
                .setParameter("offset", pageable.getOffset())
                .setParameter("size", pageable.getPageSize())
                .getResultList();

        Long total = (Long) em.createNativeQuery(
                        "SELECT COUNT(*) FROM tracks WHERE (" +
                                "MATCH(title) AGAINST(:keyword IN BOOLEAN MODE) OR artist_id IN (:ids)) " +
                                "AND deleted_at IS NULL AND status = 'PUBLISHED'")
                .setParameter("keyword", keyword)
                .setParameter("ids", ids)
                .getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page<Album> searchAlbums(String keyword, List<Long> artistIds, Pageable pageable) {
        List<Long> ids = artistIds.isEmpty() ? List.of(-1L) : artistIds;

        List<Album> results = em.createNativeQuery(
                        "SELECT * FROM albums WHERE (" +
                                "MATCH(title) AGAINST(:keyword IN BOOLEAN MODE) OR artist_id IN (:ids))" +
                                " AND deleted_at IS NULL AND status = 'PUBLISHED' LIMIT :offset, :size",
                        Album.class)
                .setParameter("keyword", keyword)
                .setParameter("ids", ids)
                .setParameter("offset", pageable.getOffset())
                .setParameter("size", pageable.getPageSize())
                .getResultList();

        Long total = (Long) em.createNativeQuery(
                        "SELECT COUNT(*) FROM albums WHERE (" +
                                "MATCH(title) AGAINST(:keyword IN BOOLEAN MODE) OR artist_id IN (:ids)) " +
                                "AND deleted_at IS NULL AND status = 'PUBLISHED'")
                .setParameter("keyword", keyword)
                .setParameter("ids", ids)
                .getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }
}
