package com.fivefy.domain.search.repository;

import com.fivefy.domain.album.entity.Album;
import com.fivefy.domain.artist.entity.Artist;
import com.fivefy.domain.track.entity.Track;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
                                    " AND deleted_at IS NULL AND status = 'ACTIVE'" +
                                    " ORDER BY " + artistSortSql(keyword) +
                                    " LIMIT :limit",
                            Artist.class)
                    .setParameter("keyword", keyword + "%")
                    .setParameter("limit", limit)
                    .getResultList();
        }
        return em.createNativeQuery(
                        "SELECT * FROM artists" +
                                " WHERE MATCH(name) AGAINST(:keyword IN BOOLEAN MODE)" +
                                " AND deleted_at IS NULL AND status = 'ACTIVE'" +
                                " ORDER BY " + artistSortSql(keyword) +
                                " LIMIT :limit",
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
        String orderClause = trackSortSql(keyword, pageable.getSort());

        if (keyword.length() < 2) {
            List<Track> results = em.createNativeQuery(
                            "SELECT * FROM tracks" +
                                    " WHERE (title LIKE :keyword OR artist_id IN (:ids))" +
                                    " AND deleted_at IS NULL AND status = 'PUBLISHED'" +
                                    " ORDER BY " + orderClause +
                                    " LIMIT :offset, :size",
                            Track.class)
                    .setParameter("keyword", keyword + "%")
                    .setParameter("ids", ids)
                    .setParameter("offset", pageable.getOffset())
                    .setParameter("size", pageable.getPageSize())
                    .getResultList();

            Long total = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tracks WHERE (title LIKE ? OR artist_id IN (" + joinedIds + "))" +
                            " AND deleted_at IS NULL AND status = 'PUBLISHED'",
                    Long.class, keyword + "%");

            return new PageImpl<>(results, pageable, total);
        }

        List<Track> results = em.createNativeQuery(
                        "SELECT * FROM tracks" +
                                " WHERE (MATCH(title) AGAINST(:keyword IN BOOLEAN MODE) OR artist_id IN (:ids))" +
                                " AND deleted_at IS NULL AND status = 'PUBLISHED'" +
                                " ORDER BY " + orderClause +
                                " LIMIT :offset, :size",
                        Track.class)
                .setParameter("keyword", keyword)
                .setParameter("ids", ids)
                .setParameter("offset", pageable.getOffset())
                .setParameter("size", pageable.getPageSize())
                .getResultList();

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tracks WHERE (MATCH(title) AGAINST(? IN BOOLEAN MODE) OR artist_id IN (" + joinedIds + "))" +
                        " AND deleted_at IS NULL AND status = 'PUBLISHED'",
                Long.class, keyword);

        return new PageImpl<>(results, pageable, total);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page<Album> searchAlbums(String keyword, List<Long> artistIds, Pageable pageable) {
        List<Long> ids = artistIds.isEmpty() ? List.of(-1L) : artistIds;
        String joinedIds = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        String orderClause = albumSortSql(keyword, pageable.getSort());

        if (keyword.length() < 2) {
            List<Album> results = em.createNativeQuery(
                            "SELECT * FROM albums" +
                                    " WHERE (title LIKE :keyword OR artist_id IN (:ids))" +
                                    " AND deleted_at IS NULL AND status = 'PUBLISHED'" +
                                    " ORDER BY " + orderClause +
                                    " LIMIT :offset, :size",
                            Album.class)
                    .setParameter("keyword", keyword + "%")
                    .setParameter("ids", ids)
                    .setParameter("offset", pageable.getOffset())
                    .setParameter("size", pageable.getPageSize())
                    .getResultList();

            Long total = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM albums WHERE (title LIKE ? OR artist_id IN (" + joinedIds + "))" +
                            " AND deleted_at IS NULL AND status = 'PUBLISHED'",
                    Long.class, keyword + "%");

            return new PageImpl<>(results, pageable, total);
        }

        List<Album> results = em.createNativeQuery(
                        "SELECT * FROM albums" +
                                " WHERE (MATCH(title) AGAINST(:keyword IN BOOLEAN MODE) OR artist_id IN (:ids))" +
                                " AND deleted_at IS NULL AND status = 'PUBLISHED'" +
                                " ORDER BY " + orderClause +
                                " LIMIT :offset, :size",
                        Album.class)
                .setParameter("keyword", keyword)
                .setParameter("ids", ids)
                .setParameter("offset", pageable.getOffset())
                .setParameter("size", pageable.getPageSize())
                .getResultList();

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM albums WHERE (MATCH(title) AGAINST(? IN BOOLEAN MODE) OR artist_id IN (" + joinedIds + "))" +
                        " AND deleted_at IS NULL AND status = 'PUBLISHED'",
                Long.class, keyword);

        return new PageImpl<>(results, pageable, total);
    }

    private String trackSortSql(String keyword, Sort sort) {
        if (sort.isSorted()) {
            for (Sort.Order order : sort) {
                return switch (order.getProperty()) {
                    case "popular" -> "play_count DESC";
                    case "latest"  -> "published_at DESC";
                    default        -> relevanceSortSql(keyword, "title");
                };
            }
        }
        return relevanceSortSql(keyword, "title");
    }

    private String albumSortSql(String keyword, Sort sort) {
        if (sort.isSorted()) {
            for (Sort.Order order : sort) {
                return switch (order.getProperty()) {
                    case "latest" -> "published_at DESC";
                    default       -> relevanceSortSql(keyword, "title");
                };
            }
        }
        return relevanceSortSql(keyword, "title");
    }

    private String artistSortSql(String keyword) {
        return relevanceSortSql(keyword, "name");
    }

    private String relevanceSortSql(String keyword, String column) {
        String safeColumn = column.matches("^[a-z_]+$") ? column : "title";
        String sanitized  = sanitizeKeyword(keyword);           // LIKE 용
        String sanitizedFt = keyword.replace("\\", "\\\\")
                .replace("'", "''");        // Full-Text 용 (% _ 이스케이프 제외)

        return String.format("""
            CASE
                WHEN %s = '%s'      THEN 2
                WHEN %s LIKE '%s%%' THEN 1
                ELSE 0
            END DESC,
            MATCH(%s) AGAINST('%s' IN BOOLEAN MODE) DESC,
            id DESC
            """, safeColumn, sanitized, safeColumn, sanitized, safeColumn, sanitizedFt);
    }

    private String sanitizeKeyword(String keyword) {
        return keyword
                .replace("\\", "\\\\")
                .replace("'", "''")
                .replace("%", "\\%")   // LIKE 와일드카드 이스케이프
                .replace("_", "\\_");  // LIKE 단일문자 와일드카드 이스케이프
    }
}