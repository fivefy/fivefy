package com.fivefy.domain.search.repository;

import com.fivefy.domain.album.entity.Album;
import com.fivefy.domain.artist.entity.Artist;
import com.fivefy.domain.track.entity.Track;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SearchQueryRepositoryImpl implements SearchQueryRepository {

    private final EntityManager em;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @SuppressWarnings("unchecked")
    public List<Artist> searchArtists(String keyword, int limit) {
        if (keyword.length() < 2) {
            return em.createNativeQuery(
                            "SELECT * FROM artists WHERE name LIKE :keyword" +
                                    " AND deleted_at IS NULL AND status = 'ACTIVE' LIMIT :limit",
                            Artist.class)
                    .setParameter("keyword", keyword + "%")
                    .setParameter("limit", limit)
                    .getResultList();
        }
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
        String joinedIds = ids.stream().map(String::valueOf).collect(Collectors.joining(","));

        if (keyword.length() < 2) {
            List<Track> results = em.createNativeQuery(
                            "SELECT * FROM tracks WHERE (title LIKE :keyword OR artist_id IN (:ids))" +
                                    " AND deleted_at IS NULL AND status = 'PUBLISHED' LIMIT :offset, :size",
                            Track.class)
                    .setParameter("keyword", keyword + "%")
                    .setParameter("ids", ids)
                    .setParameter("offset", pageable.getOffset())
                    .setParameter("size", pageable.getPageSize())
                    .getResultList();

            Long total = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tracks WHERE (title LIKE ? OR artist_id IN (" + joinedIds + "))" +
                            " AND deleted_at IS NULL AND status = 'PUBLISHED'",
                    Long.class,
                    keyword + "%");

            return new PageImpl<>(results, pageable, total);
        }

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

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tracks WHERE (" +
                        "MATCH(title) AGAINST(? IN BOOLEAN MODE) OR artist_id IN (" + joinedIds + "))" +
                        " AND deleted_at IS NULL AND status = 'PUBLISHED'",
                Long.class,
                keyword);

        return new PageImpl<>(results, pageable, total);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page<Album> searchAlbums(String keyword, List<Long> artistIds, Pageable pageable) {
        List<Long> ids = artistIds.isEmpty() ? List.of(-1L) : artistIds;
        String joinedIds = ids.stream().map(String::valueOf).collect(Collectors.joining(","));

        if (keyword.length() < 2) {
            List<Album> results = em.createNativeQuery(
                            "SELECT * FROM albums WHERE (title LIKE :keyword OR artist_id IN (:ids))" +
                                    " AND deleted_at IS NULL AND status = 'PUBLISHED' LIMIT :offset, :size",
                            Album.class)
                    .setParameter("keyword", keyword + "%")
                    .setParameter("ids", ids)
                    .setParameter("offset", pageable.getOffset())
                    .setParameter("size", pageable.getPageSize())
                    .getResultList();

            Long total = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM albums WHERE (title LIKE ? OR artist_id IN (" + joinedIds + "))" +
                            " AND deleted_at IS NULL AND status = 'PUBLISHED'",
                    Long.class,
                    keyword + "%");

            return new PageImpl<>(results, pageable, total);
        }

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

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM albums WHERE (" +
                        "MATCH(title) AGAINST(? IN BOOLEAN MODE) OR artist_id IN (" + joinedIds + "))" +
                        " AND deleted_at IS NULL AND status = 'PUBLISHED'",
                Long.class,
                keyword);

        return new PageImpl<>(results, pageable, total);
    }
}