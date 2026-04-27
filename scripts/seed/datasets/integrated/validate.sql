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
