SELECT '==================== 1. ROW COUNT ====================' AS section;

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


SELECT '==================== 2. USER DISTRIBUTION ====================' AS section;

SELECT
    status,
    COUNT(*) AS count,
    ROUND(COUNT(*) / (SELECT COUNT(*) FROM users) * 100, 2) AS ratio
FROM users
GROUP BY status
ORDER BY status;

SELECT
    role,
    COUNT(*) AS count,
    ROUND(COUNT(*) / (SELECT COUNT(*) FROM users) * 100, 2) AS ratio
FROM users
GROUP BY role
ORDER BY role;


SELECT '==================== 3. ARTIST DISTRIBUTION ====================' AS section;

SELECT
    status,
    COUNT(*) AS count,
    ROUND(COUNT(*) / (SELECT COUNT(*) FROM artists) * 100, 2) AS ratio
FROM artists
GROUP BY status
ORDER BY status;

SELECT
    artist_type,
    COUNT(*) AS count,
    ROUND(COUNT(*) / (SELECT COUNT(*) FROM artists) * 100, 2) AS ratio
FROM artists
GROUP BY artist_type
ORDER BY artist_type;


SELECT '==================== 4. ALBUM DISTRIBUTION ====================' AS section;

SELECT
    status,
    COUNT(*) AS count,
    ROUND(COUNT(*) / (SELECT COUNT(*) FROM albums) * 100, 2) AS ratio
FROM albums
GROUP BY status
ORDER BY status;

SELECT
    'albums_deleted_ratio' AS check_name,
    COUNT(*) AS total_count,
    SUM(deleted_at IS NULL) AS null_count,
    SUM(deleted_at IS NOT NULL) AS not_null_count,
    ROUND(SUM(deleted_at IS NOT NULL) / COUNT(*) * 100, 2) AS not_null_ratio
FROM albums;


SELECT '==================== 5. TRACK DISTRIBUTION ====================' AS section;

SELECT
    status,
    COUNT(*) AS count,
    ROUND(COUNT(*) / (SELECT COUNT(*) FROM tracks) * 100, 2) AS ratio
FROM tracks
GROUP BY status
ORDER BY status;

SELECT
    track_type,
    COUNT(*) AS count,
    ROUND(COUNT(*) / (SELECT COUNT(*) FROM tracks) * 100, 2) AS ratio
FROM tracks
GROUP BY track_type
ORDER BY track_type;

SELECT
    'tracks_deleted_ratio' AS check_name,
    COUNT(*) AS total_count,
    SUM(deleted_at IS NULL) AS null_count,
    SUM(deleted_at IS NOT NULL) AS not_null_count,
    ROUND(SUM(deleted_at IS NOT NULL) / COUNT(*) * 100, 2) AS not_null_ratio
FROM tracks;

SELECT
    'tracks_published_at_ratio' AS check_name,
    COUNT(*) AS total_count,
    SUM(published_at IS NULL) AS null_count,
    SUM(published_at IS NOT NULL) AS not_null_count,
    ROUND(SUM(published_at IS NOT NULL) / COUNT(*) * 100, 2) AS not_null_ratio
FROM tracks;


SELECT '==================== 6. TRACK COMMENT DISTRIBUTION ====================' AS section;

SELECT
    'track_comments_deleted_ratio' AS check_name,
    COUNT(*) AS total_count,
    SUM(deleted_at IS NULL) AS null_count,
    SUM(deleted_at IS NOT NULL) AS not_null_count,
    ROUND(SUM(deleted_at IS NOT NULL) / COUNT(*) * 100, 2) AS not_null_ratio
FROM track_comments;

SELECT
    track_id,
    COUNT(*) AS comment_count
FROM track_comments
GROUP BY track_id
ORDER BY comment_count DESC
LIMIT 20;


SELECT '==================== 7. APPLICATION DISTRIBUTION ====================' AS section;

SELECT 'artist_applications_status' AS target;

SELECT
    status,
    COUNT(*) AS count,
    ROUND(COUNT(*) / (SELECT COUNT(*) FROM artist_applications) * 100, 2) AS ratio
FROM artist_applications
GROUP BY status
ORDER BY status;

SELECT 'album_applications_status' AS target;

SELECT
    status,
    COUNT(*) AS count,
    ROUND(COUNT(*) / (SELECT COUNT(*) FROM album_applications) * 100, 2) AS ratio
FROM album_applications
GROUP BY status
ORDER BY status;

SELECT 'track_applications_status' AS target;

SELECT
    status,
    COUNT(*) AS count,
    ROUND(COUNT(*) / (SELECT COUNT(*) FROM track_applications) * 100, 2) AS ratio
FROM track_applications
GROUP BY status
ORDER BY status;


SELECT '==================== 8. NULL SAFETY CHECK ====================' AS section;

SELECT
    'tracks_deleted_at' AS target,
    COUNT(*) AS total_count,
    SUM(deleted_at IS NULL) AS null_count,
    SUM(deleted_at IS NOT NULL) AS not_null_count
FROM tracks
UNION ALL
SELECT
    'albums_deleted_at',
    COUNT(*),
    SUM(deleted_at IS NULL),
    SUM(deleted_at IS NOT NULL)
FROM albums
UNION ALL
SELECT
    'track_comments_deleted_at',
    COUNT(*),
    SUM(deleted_at IS NULL),
    SUM(deleted_at IS NOT NULL)
FROM track_comments
UNION ALL
SELECT
    'tracks_published_at',
    COUNT(*),
    SUM(published_at IS NULL),
    SUM(published_at IS NOT NULL)
FROM tracks;