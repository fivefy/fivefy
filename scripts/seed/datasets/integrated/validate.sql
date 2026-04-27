SELECT '==================== 1. ROW COUNT ====================' AS section;

SELECT 'users' AS table_name, FORMAT(COUNT(*), 0) AS row_count FROM users
UNION ALL
SELECT 'wallets', FORMAT(COUNT(*), 0) FROM wallets
UNION ALL
SELECT 'point_histories', FORMAT(COUNT(*), 0) FROM point_histories
UNION ALL
SELECT 'artists', FORMAT(COUNT(*), 0) FROM artists
UNION ALL
SELECT 'albums', FORMAT(COUNT(*), 0) FROM albums
UNION ALL
SELECT 'tracks', FORMAT(COUNT(*), 0) FROM tracks
UNION ALL
SELECT 'track_comments', FORMAT(COUNT(*), 0) FROM track_comments
UNION ALL
SELECT 'artist_applications', FORMAT(COUNT(*), 0) FROM artist_applications
UNION ALL
SELECT 'album_applications', FORMAT(COUNT(*), 0) FROM album_applications
UNION ALL
SELECT 'track_applications', FORMAT(COUNT(*), 0) FROM track_applications;


SELECT '==================== 2. USER DISTRIBUTION ====================' AS section;

SELECT
    status,
    FORMAT(COUNT(*), 0) AS row_count,
    CONCAT(ROUND(COUNT(*) / (SELECT COUNT(*) FROM users) * 100, 2), '%') AS ratio_percent
FROM users
GROUP BY status
ORDER BY status;

SELECT
    role,
    FORMAT(COUNT(*), 0) AS row_count,
    CONCAT(ROUND(COUNT(*) / (SELECT COUNT(*) FROM users) * 100, 2), '%') AS ratio_percent
FROM users
GROUP BY role
ORDER BY role;


SELECT '==================== 3. WALLET DISTRIBUTION ====================' AS section;

SELECT
    'wallet_balance_summary' AS check_name,
    FORMAT(COUNT(*), 0) AS wallet_count,
    FORMAT(SUM(balance), 0) AS total_paid_balance,
    FORMAT(SUM(event_balance), 0) AS total_event_balance,
    FORMAT(SUM(total_balance), 0) AS total_balance,
    FORMAT(ROUND(AVG(total_balance), 0), 0) AS avg_total_balance
FROM wallets;

SELECT
    point_type,
    FORMAT(COUNT(*), 0) AS row_count,
    CONCAT(ROUND(COUNT(*) / (SELECT COUNT(*) FROM point_histories) * 100, 2), '%') AS ratio_percent
FROM point_histories
GROUP BY point_type
ORDER BY point_type;

SELECT
    point_history_type,
    FORMAT(COUNT(*), 0) AS row_count,
    CONCAT(ROUND(COUNT(*) / (SELECT COUNT(*) FROM point_histories) * 100, 2), '%') AS ratio_percent
FROM point_histories
GROUP BY point_history_type
ORDER BY point_history_type;

SELECT
    'wallet_point_history_relation_check' AS check_name,
    FORMAT(COUNT(DISTINCT w.id), 0) AS wallet_count,
    FORMAT(COUNT(DISTINCT ph.wallet_id), 0) AS wallet_with_history_count,
    FORMAT(COUNT(ph.id), 0) AS point_history_count
FROM wallets w
         LEFT JOIN point_histories ph ON ph.wallet_id = w.id;


SELECT '==================== 4. ARTIST DISTRIBUTION ====================' AS section;

SELECT
    status,
    FORMAT(COUNT(*), 0) AS row_count,
    CONCAT(ROUND(COUNT(*) / (SELECT COUNT(*) FROM artists) * 100, 2), '%') AS ratio_percent
FROM artists
GROUP BY status
ORDER BY status;

SELECT
    artist_type,
    FORMAT(COUNT(*), 0) AS row_count,
    CONCAT(ROUND(COUNT(*) / (SELECT COUNT(*) FROM artists) * 100, 2), '%') AS ratio_percent
FROM artists
GROUP BY artist_type
ORDER BY artist_type;

SELECT
    'artists_deleted_ratio' AS check_name,
    FORMAT(COUNT(*), 0) AS total_count,
    FORMAT(SUM(deleted_at IS NULL), 0) AS null_count,
    FORMAT(SUM(deleted_at IS NOT NULL), 0) AS not_null_count,
    CONCAT(ROUND(SUM(deleted_at IS NOT NULL) / COUNT(*) * 100, 2), '%') AS not_null_ratio
FROM artists;


SELECT '==================== 5. ALBUM DISTRIBUTION ====================' AS section;

SELECT
    status,
    FORMAT(COUNT(*), 0) AS row_count,
    CONCAT(ROUND(COUNT(*) / (SELECT COUNT(*) FROM albums) * 100, 2), '%') AS ratio_percent
FROM albums
GROUP BY status
ORDER BY status;

SELECT
    'albums_deleted_ratio' AS check_name,
    FORMAT(COUNT(*), 0) AS total_count,
    FORMAT(SUM(deleted_at IS NULL), 0) AS null_count,
    FORMAT(SUM(deleted_at IS NOT NULL), 0) AS not_null_count,
    CONCAT(ROUND(SUM(deleted_at IS NOT NULL) / COUNT(*) * 100, 2), '%') AS not_null_ratio
FROM albums;

SELECT
    'albums_published_at_ratio' AS check_name,
    FORMAT(COUNT(*), 0) AS total_count,
    FORMAT(SUM(published_at IS NULL), 0) AS null_count,
    FORMAT(SUM(published_at IS NOT NULL), 0) AS not_null_count,
    CONCAT(ROUND(SUM(published_at IS NOT NULL) / COUNT(*) * 100, 2), '%') AS not_null_ratio
FROM albums;


SELECT '==================== 6. TRACK DISTRIBUTION ====================' AS section;

SELECT
    status,
    FORMAT(COUNT(*), 0) AS row_count,
    CONCAT(ROUND(COUNT(*) / (SELECT COUNT(*) FROM tracks) * 100, 2), '%') AS ratio_percent
FROM tracks
GROUP BY status
ORDER BY status;

SELECT
    track_type,
    FORMAT(COUNT(*), 0) AS row_count,
    CONCAT(ROUND(COUNT(*) / (SELECT COUNT(*) FROM tracks) * 100, 2), '%') AS ratio_percent
FROM tracks
GROUP BY track_type
ORDER BY track_type;

SELECT
    'tracks_deleted_ratio' AS check_name,
    FORMAT(COUNT(*), 0) AS total_count,
    FORMAT(SUM(deleted_at IS NULL), 0) AS null_count,
    FORMAT(SUM(deleted_at IS NOT NULL), 0) AS not_null_count,
    CONCAT(ROUND(SUM(deleted_at IS NOT NULL) / COUNT(*) * 100, 2), '%') AS not_null_ratio
FROM tracks;

SELECT
    'tracks_published_at_ratio' AS check_name,
    FORMAT(COUNT(*), 0) AS total_count,
    FORMAT(SUM(published_at IS NULL), 0) AS null_count,
    FORMAT(SUM(published_at IS NOT NULL), 0) AS not_null_count,
    CONCAT(ROUND(SUM(published_at IS NOT NULL) / COUNT(*) * 100, 2), '%') AS not_null_ratio
FROM tracks;

SELECT
    track_type,
    FORMAT(COUNT(*), 0) AS total_count,
    FORMAT(SUM(artist_id IS NULL), 0) AS artist_id_null_count,
    FORMAT(SUM(album_id IS NULL), 0) AS album_id_null_count,
    FORMAT(SUM(track_number IS NULL), 0) AS track_number_null_count
FROM tracks
GROUP BY track_type
ORDER BY track_type;


SELECT '==================== 7. TRACK COMMENT DISTRIBUTION ====================' AS section;

SELECT
    'track_comments_deleted_ratio' AS check_name,
    FORMAT(COUNT(*), 0) AS total_count,
    FORMAT(SUM(deleted_at IS NULL), 0) AS null_count,
    FORMAT(SUM(deleted_at IS NOT NULL), 0) AS not_null_count,
    CONCAT(ROUND(SUM(deleted_at IS NOT NULL) / COUNT(*) * 100, 2), '%') AS not_null_ratio
FROM track_comments;

SELECT
    'top_20_track_comment_distribution' AS check_name,
    FORMAT(SUM(comment_count), 0) AS top_20_comment_count,
    FORMAT((SELECT COUNT(*) FROM track_comments), 0) AS total_comment_count,
    CONCAT(ROUND(SUM(comment_count) / (SELECT COUNT(*) FROM track_comments) * 100, 2), '%') AS top_20_ratio
FROM (
         SELECT
             track_id,
             COUNT(*) AS comment_count
         FROM track_comments
         GROUP BY track_id
         ORDER BY comment_count DESC
         LIMIT 20
     ) top_comments;

SELECT
    track_id,
    FORMAT(COUNT(*), 0) AS comment_count
FROM track_comments
GROUP BY track_id
ORDER BY COUNT(*) DESC
LIMIT 20;


SELECT '==================== 8. APPLICATION DISTRIBUTION ====================' AS section;

SELECT 'artist_applications_status' AS target;

SELECT
    status,
    FORMAT(COUNT(*), 0) AS row_count,
    CONCAT(ROUND(COUNT(*) / (SELECT COUNT(*) FROM artist_applications) * 100, 2), '%') AS ratio_percent
FROM artist_applications
GROUP BY status
ORDER BY status;

SELECT 'album_applications_status' AS target;

SELECT
    status,
    FORMAT(COUNT(*), 0) AS row_count,
    CONCAT(ROUND(COUNT(*) / (SELECT COUNT(*) FROM album_applications) * 100, 2), '%') AS ratio_percent
FROM album_applications
GROUP BY status
ORDER BY status;

SELECT 'track_applications_status' AS target;

SELECT
    status,
    FORMAT(COUNT(*), 0) AS row_count,
    CONCAT(ROUND(COUNT(*) / (SELECT COUNT(*) FROM track_applications) * 100, 2), '%') AS ratio_percent
FROM track_applications
GROUP BY status
ORDER BY status;

SELECT
    'artist_applications_review_field_check' AS check_name,
    FORMAT(SUM(status = 'PENDING' AND reviewed_at IS NULL AND reviewed_by_admin_id IS NULL), 0) AS pending_without_review_count,
    FORMAT(SUM(status IN ('APPROVED', 'REJECTED') AND reviewed_at IS NOT NULL AND reviewed_by_admin_id IS NOT NULL), 0) AS reviewed_status_with_review_count,
    FORMAT(SUM(status = 'REJECTED' AND rejection_reason IS NOT NULL), 0) AS rejected_with_reason_count
FROM artist_applications;

SELECT
    'album_applications_review_field_check' AS check_name,
    FORMAT(SUM(status = 'PENDING' AND reviewed_at IS NULL AND reviewed_by_admin_id IS NULL), 0) AS pending_without_review_count,
    FORMAT(SUM(status IN ('APPROVED', 'REJECTED') AND reviewed_at IS NOT NULL AND reviewed_by_admin_id IS NOT NULL), 0) AS reviewed_status_with_review_count,
    FORMAT(SUM(status = 'REJECTED' AND rejection_reason IS NOT NULL), 0) AS rejected_with_reason_count
FROM album_applications;

SELECT
    'track_applications_review_field_check' AS check_name,
    FORMAT(SUM(status = 'PENDING' AND reviewed_at IS NULL AND reviewed_by_admin_id IS NULL), 0) AS pending_without_review_count,
    FORMAT(SUM(status IN ('APPROVED', 'REJECTED') AND reviewed_at IS NOT NULL AND reviewed_by_admin_id IS NOT NULL), 0) AS reviewed_status_with_review_count,
    FORMAT(SUM(status = 'REJECTED' AND rejection_reason IS NOT NULL), 0) AS rejected_with_reason_count
FROM track_applications;


SELECT '==================== 9. NULL SAFETY CHECK ====================' AS section;

SELECT
    'tracks_deleted_at' AS target,
    FORMAT(COUNT(*), 0) AS total_count,
    FORMAT(SUM(deleted_at IS NULL), 0) AS null_count,
    FORMAT(SUM(deleted_at IS NOT NULL), 0) AS not_null_count
FROM tracks
UNION ALL
SELECT
    'albums_deleted_at',
    FORMAT(COUNT(*), 0),
    FORMAT(SUM(deleted_at IS NULL), 0),
    FORMAT(SUM(deleted_at IS NOT NULL), 0)
FROM albums
UNION ALL
SELECT
    'artists_deleted_at',
    FORMAT(COUNT(*), 0),
    FORMAT(SUM(deleted_at IS NULL), 0),
    FORMAT(SUM(deleted_at IS NOT NULL), 0)
FROM artists
UNION ALL
SELECT
    'track_comments_deleted_at',
    FORMAT(COUNT(*), 0),
    FORMAT(SUM(deleted_at IS NULL), 0),
    FORMAT(SUM(deleted_at IS NOT NULL), 0)
FROM track_comments
UNION ALL
SELECT
    'tracks_published_at',
    FORMAT(COUNT(*), 0),
    FORMAT(SUM(published_at IS NULL), 0),
    FORMAT(SUM(published_at IS NOT NULL), 0)
FROM tracks
UNION ALL
SELECT
    'albums_published_at',
    FORMAT(COUNT(*), 0),
    FORMAT(SUM(published_at IS NULL), 0),
    FORMAT(SUM(published_at IS NOT NULL), 0)
FROM albums;