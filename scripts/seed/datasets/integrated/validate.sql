SELECT 'users' AS table_name, COUNT(*) AS row_count FROM users
UNION ALL SELECT 'wallets', COUNT(*) FROM wallets
UNION ALL SELECT 'point_histories', COUNT(*) FROM point_histories
UNION ALL SELECT 'billing_keys', COUNT(*) FROM billing_keys
UNION ALL SELECT 'cash_orders', COUNT(*) FROM cash_orders
UNION ALL SELECT 'point_orders', COUNT(*) FROM point_orders
UNION ALL SELECT 'payments', COUNT(*) FROM payments
UNION ALL SELECT 'subscriptions', COUNT(*) FROM subscriptions
UNION ALL SELECT 'artists', COUNT(*) FROM artists
UNION ALL SELECT 'albums', COUNT(*) FROM albums
UNION ALL SELECT 'tracks', COUNT(*) FROM tracks
UNION ALL SELECT 'playlists', COUNT(*) FROM playlists
UNION ALL SELECT 'playlist_tracks', COUNT(*) FROM playlist_tracks
UNION ALL SELECT 'track_comments', COUNT(*) FROM track_comments
UNION ALL SELECT 'artist_applications', COUNT(*) FROM artist_applications
UNION ALL SELECT 'album_applications', COUNT(*) FROM album_applications
UNION ALL SELECT 'track_applications', COUNT(*) FROM track_applications;

SELECT 'playlist_tracks_invalid_playlist' AS check_name, COUNT(*) AS invalid_count
FROM playlist_tracks pt
LEFT JOIN playlists p ON pt.playlist_id = p.id
WHERE p.id IS NULL;

SELECT 'playlist_tracks_invalid_track' AS check_name, COUNT(*) AS invalid_count
FROM playlist_tracks pt
LEFT JOIN tracks t ON pt.track_id = t.id
WHERE t.id IS NULL;


SELECT '==================== BEHAVIOR LOG FK CHECK ====================' AS section;
SELECT 'likes_invalid_user' AS check_name, COUNT(*) AS invalid_count
FROM likes l
LEFT JOIN users u ON l.user_id = u.id
WHERE u.id IS NULL
UNION ALL
SELECT 'likes_invalid_track', COUNT(*)
FROM likes l
LEFT JOIN tracks t ON l.target_id = t.id
WHERE l.target_type = 'TRACK' AND t.id IS NULL
UNION ALL
SELECT 'follows_invalid_artist', COUNT(*)
FROM follows f
LEFT JOIN artists a ON f.artist_id = a.id
WHERE a.id IS NULL
UNION ALL
SELECT 'follows_invalid_user', COUNT(*)
FROM follows f
LEFT JOIN users u ON f.user_id = u.id
WHERE u.id IS NULL
UNION ALL
SELECT 'playbacks_invalid_playlist', COUNT(*)
FROM playbacks p
LEFT JOIN playlists pl ON p.playlist_id = pl.id
WHERE pl.id IS NULL
UNION ALL
SELECT 'playbacks_invalid_track', COUNT(*)
FROM playbacks p
LEFT JOIN tracks t ON p.track_id = t.id
WHERE t.id IS NULL
UNION ALL
SELECT 'playbacks_invalid_user', COUNT(*)
FROM playbacks p
LEFT JOIN users u ON p.user_id = u.id
WHERE u.id IS NULL
UNION ALL
SELECT 'search_histories_invalid_user', COUNT(*)
FROM search_histories sh
LEFT JOIN users u ON sh.user_id = u.id
WHERE sh.user_id IS NOT NULL AND u.id IS NULL;

SELECT '==================== BEHAVIOR LOG ROW COUNT ====================' AS section;
SELECT 'likes' AS table_name, COUNT(*) AS row_count FROM likes
UNION ALL
SELECT 'follows', COUNT(*) FROM follows
UNION ALL
SELECT 'playbacks', COUNT(*) FROM playbacks
UNION ALL
SELECT 'search_histories', COUNT(*) FROM search_histories;
