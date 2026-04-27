SELECT 'users' AS table_name, COUNT(*) AS row_count FROM users
UNION ALL
SELECT 'artists', COUNT(*) FROM artists
UNION ALL
SELECT 'albums', COUNT(*) FROM albums
UNION ALL
SELECT 'tracks', COUNT(*) FROM tracks
UNION ALL
SELECT 'track_comments', COUNT(*) FROM track_comments
UNION ALL
SELECT 'artist_applications', COUNT(*) FROM artist_applications
UNION ALL
SELECT 'album_applications', COUNT(*) FROM album_applications
UNION ALL
SELECT 'track_applications', COUNT(*) FROM track_applications;

SELECT status, COUNT(*) AS count
FROM users
GROUP BY status
ORDER BY status;

SELECT role, COUNT(*) AS count
FROM users
GROUP BY role
ORDER BY role;

SELECT status, COUNT(*) AS count
FROM artists
GROUP BY status
ORDER BY status;

SELECT artist_type, COUNT(*) AS count
FROM artists
GROUP BY artist_type
ORDER BY artist_type;

SELECT status, COUNT(*) AS count
FROM albums
GROUP BY status
ORDER BY status;

SELECT status, COUNT(*) AS count
FROM tracks
GROUP BY status
ORDER BY status;

SELECT track_type, COUNT(*) AS count
FROM tracks
GROUP BY track_type
ORDER BY track_type;

SELECT status, COUNT(*) AS count
FROM artist_applications
GROUP BY status
ORDER BY status;

SELECT status, COUNT(*) AS count
FROM album_applications
GROUP BY status
ORDER BY status;

SELECT status, COUNT(*) AS count
FROM track_applications
GROUP BY status
ORDER BY status;

SELECT
    'tracks_deleted_ratio' AS check_name,
    COUNT(*) AS total_count,
    SUM(deleted_at IS NULL) AS null_count,
    SUM(deleted_at IS NOT NULL) AS not_null_count,
    ROUND(SUM(deleted_at IS NOT NULL) / COUNT(*) * 100, 2) AS not_null_ratio
FROM tracks;

SELECT
    'track_comments_deleted_ratio' AS check_name,
    COUNT(*) AS total_count,
    SUM(deleted_at IS NULL) AS null_count,
    SUM(deleted_at IS NOT NULL) AS not_null_count,
    ROUND(SUM(deleted_at IS NOT NULL) / COUNT(*) * 100, 2) AS not_null_ratio
FROM track_comments;

SELECT
    'albums_deleted_ratio' AS check_name,
    COUNT(*) AS total_count,
    SUM(deleted_at IS NULL) AS null_count,
    SUM(deleted_at IS NOT NULL) AS not_null_count,
    ROUND(SUM(deleted_at IS NOT NULL) / COUNT(*) * 100, 2) AS not_null_ratio
FROM albums;

SELECT
    'tracks_published_at_ratio' AS check_name,
    COUNT(*) AS total_count,
    SUM(published_at IS NULL) AS null_count,
    SUM(published_at IS NOT NULL) AS not_null_count,
    ROUND(SUM(published_at IS NOT NULL) / COUNT(*) * 100, 2) AS not_null_ratio
FROM tracks;

SELECT
    track_id,
    COUNT(*) AS comment_count
FROM track_comments
GROUP BY track_id
ORDER BY comment_count DESC
LIMIT 20;