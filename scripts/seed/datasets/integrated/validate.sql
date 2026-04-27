SELECT '==================== 1. ROW COUNT ====================' AS section;

SELECT 'users' AS table_name, FORMAT(COUNT(*), 0) AS row_count FROM users
UNION ALL SELECT 'wallets', FORMAT(COUNT(*), 0) FROM wallets
UNION ALL SELECT 'point_histories', FORMAT(COUNT(*), 0) FROM point_histories
UNION ALL SELECT 'billing_keys', FORMAT(COUNT(*), 0) FROM billing_keys
UNION ALL SELECT 'cash_orders', FORMAT(COUNT(*), 0) FROM cash_orders
UNION ALL SELECT 'point_orders', FORMAT(COUNT(*), 0) FROM point_orders
UNION ALL SELECT 'payments', FORMAT(COUNT(*), 0) FROM payments
UNION ALL SELECT 'subscriptions', FORMAT(COUNT(*), 0) FROM subscriptions
UNION ALL SELECT 'artists', FORMAT(COUNT(*), 0) FROM artists
UNION ALL SELECT 'albums', FORMAT(COUNT(*), 0) FROM albums
UNION ALL SELECT 'tracks', FORMAT(COUNT(*), 0) FROM tracks
UNION ALL SELECT 'track_comments', FORMAT(COUNT(*), 0) FROM track_comments
UNION ALL SELECT 'artist_applications', FORMAT(COUNT(*), 0) FROM artist_applications
UNION ALL SELECT 'album_applications', FORMAT(COUNT(*), 0) FROM album_applications
UNION ALL SELECT 'track_applications', FORMAT(COUNT(*), 0) FROM track_applications;

SELECT '==================== 2. BILLING / SUBSCRIPTION DISTRIBUTION ====================' AS section;
SELECT status, FORMAT(COUNT(*), 0) AS row_count, CONCAT(ROUND(COUNT(*) / (SELECT COUNT(*) FROM cash_orders) * 100, 2), '%') AS ratio_percent FROM cash_orders GROUP BY status ORDER BY status;
SELECT product_type, FORMAT(COUNT(*), 0) AS row_count FROM cash_orders GROUP BY product_type ORDER BY product_type;
SELECT status, FORMAT(COUNT(*), 0) AS row_count, CONCAT(ROUND(COUNT(*) / (SELECT COUNT(*) FROM point_orders) * 100, 2), '%') AS ratio_percent FROM point_orders GROUP BY status ORDER BY status;
SELECT status, FORMAT(COUNT(*), 0) AS row_count, CONCAT(ROUND(COUNT(*) / (SELECT COUNT(*) FROM payments) * 100, 2), '%') AS ratio_percent FROM payments GROUP BY status ORDER BY status;
SELECT status, FORMAT(COUNT(*), 0) AS row_count, CONCAT(ROUND(COUNT(*) / (SELECT COUNT(*) FROM subscriptions) * 100, 2), '%') AS ratio_percent FROM subscriptions GROUP BY status ORDER BY status;

SELECT '==================== 3. WALLET DISTRIBUTION ====================' AS section;
SELECT 'wallet_balance_summary' AS check_name, FORMAT(COUNT(*), 0) AS wallet_count, FORMAT(SUM(balance), 0) AS total_paid_balance, FORMAT(SUM(event_balance), 0) AS total_event_balance, FORMAT(SUM(total_balance), 0) AS total_balance, FORMAT(ROUND(AVG(total_balance), 0), 0) AS avg_total_balance FROM wallets;
SELECT point_type, FORMAT(COUNT(*), 0) AS row_count FROM point_histories GROUP BY point_type ORDER BY point_type;
SELECT point_history_type, FORMAT(COUNT(*), 0) AS row_count FROM point_histories GROUP BY point_history_type ORDER BY point_history_type;

SELECT '==================== 4. USER DISTRIBUTION ====================' AS section;
SELECT status, FORMAT(COUNT(*), 0) AS row_count, CONCAT(ROUND(COUNT(*) / (SELECT COUNT(*) FROM users) * 100, 2), '%') AS ratio_percent FROM users GROUP BY status ORDER BY status;
SELECT role, FORMAT(COUNT(*), 0) AS row_count, CONCAT(ROUND(COUNT(*) / (SELECT COUNT(*) FROM users) * 100, 2), '%') AS ratio_percent FROM users GROUP BY role ORDER BY role;

SELECT '==================== 5. MUSIC CONTENT DISTRIBUTION ====================' AS section;
SELECT status, FORMAT(COUNT(*), 0) AS row_count FROM artists GROUP BY status ORDER BY status;
SELECT status, FORMAT(COUNT(*), 0) AS row_count FROM albums GROUP BY status ORDER BY status;
SELECT status, FORMAT(COUNT(*), 0) AS row_count FROM tracks GROUP BY status ORDER BY status;
SELECT track_type, FORMAT(COUNT(*), 0) AS row_count FROM tracks GROUP BY track_type ORDER BY track_type;
SELECT 'track_comments_deleted_ratio' AS check_name, FORMAT(COUNT(*), 0) AS total_count, FORMAT(SUM(deleted_at IS NULL), 0) AS null_count, FORMAT(SUM(deleted_at IS NOT NULL), 0) AS not_null_count, CONCAT(ROUND(SUM(deleted_at IS NOT NULL) / COUNT(*) * 100, 2), '%') AS not_null_ratio FROM track_comments;

SELECT '==================== 6. APPLICATION DISTRIBUTION ====================' AS section;
SELECT 'artist_applications_status' AS target;
SELECT status, FORMAT(COUNT(*), 0) AS row_count FROM artist_applications GROUP BY status ORDER BY status;
SELECT 'album_applications_status' AS target;
SELECT status, FORMAT(COUNT(*), 0) AS row_count FROM album_applications GROUP BY status ORDER BY status;
SELECT 'track_applications_status' AS target;
SELECT status, FORMAT(COUNT(*), 0) AS row_count FROM track_applications GROUP BY status ORDER BY status;

SELECT '==================== 7. NULL SAFETY CHECK ====================' AS section;
SELECT 'payments_paid_at' AS target, FORMAT(COUNT(*), 0) AS total_count, FORMAT(SUM(paid_at IS NULL), 0) AS null_count, FORMAT(SUM(paid_at IS NOT NULL), 0) AS not_null_count FROM payments
UNION ALL SELECT 'payments_refunded_at', FORMAT(COUNT(*), 0), FORMAT(SUM(refunded_at IS NULL), 0), FORMAT(SUM(refunded_at IS NOT NULL), 0) FROM payments
UNION ALL SELECT 'subscriptions_next_billing_date', FORMAT(COUNT(*), 0), FORMAT(SUM(next_billing_date IS NULL), 0), FORMAT(SUM(next_billing_date IS NOT NULL), 0) FROM subscriptions
UNION ALL SELECT 'tracks_deleted_at', FORMAT(COUNT(*), 0), FORMAT(SUM(deleted_at IS NULL), 0), FORMAT(SUM(deleted_at IS NOT NULL), 0) FROM tracks
UNION ALL SELECT 'track_comments_deleted_at', FORMAT(COUNT(*), 0), FORMAT(SUM(deleted_at IS NULL), 0), FORMAT(SUM(deleted_at IS NOT NULL), 0) FROM track_comments;
