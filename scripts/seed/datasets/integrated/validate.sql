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


SELECT '==================== FK CHECK ====================' AS section;
SELECT 'wallets_invalid_user' AS check_name, COUNT(*) AS invalid_count FROM wallets w LEFT JOIN users u ON w.user_id = u.id WHERE u.id IS NULL
UNION ALL SELECT 'point_histories_invalid_wallet', COUNT(*) FROM point_histories ph LEFT JOIN wallets w ON ph.wallet_id = w.id WHERE w.id IS NULL
UNION ALL SELECT 'billing_keys_invalid_user', COUNT(*) FROM billing_keys bk LEFT JOIN users u ON bk.user_id = u.id WHERE u.id IS NULL
UNION ALL SELECT 'cash_orders_invalid_user', COUNT(*) FROM cash_orders co LEFT JOIN users u ON co.user_id = u.id WHERE u.id IS NULL
UNION ALL SELECT 'point_orders_invalid_user', COUNT(*) FROM point_orders po LEFT JOIN users u ON po.user_id = u.id WHERE u.id IS NULL
UNION ALL SELECT 'payments_invalid_user', COUNT(*) FROM payments p LEFT JOIN users u ON p.user_id = u.id WHERE u.id IS NULL
UNION ALL SELECT 'payments_invalid_cash_order', COUNT(*) FROM payments p LEFT JOIN cash_orders co ON p.cash_order_id = co.id WHERE co.id IS NULL
UNION ALL SELECT 'subscriptions_invalid_point_order', COUNT(*) FROM subscriptions s LEFT JOIN point_orders po ON s.point_order_id = po.id WHERE po.id IS NULL
UNION ALL SELECT 'artists_invalid_owner_user', COUNT(*) FROM artists a LEFT JOIN users u ON a.owner_user_id = u.id WHERE u.id IS NULL
UNION ALL SELECT 'albums_invalid_artist', COUNT(*) FROM albums a LEFT JOIN artists ar ON a.artist_id = ar.id WHERE ar.id IS NULL
UNION ALL SELECT 'tracks_invalid_owner_user', COUNT(*) FROM tracks t LEFT JOIN users u ON t.owner_user_id = u.id WHERE u.id IS NULL
UNION ALL SELECT 'tracks_invalid_artist', COUNT(*) FROM tracks t LEFT JOIN artists a ON t.artist_id = a.id WHERE t.artist_id IS NOT NULL AND a.id IS NULL
UNION ALL SELECT 'tracks_invalid_album', COUNT(*) FROM tracks t LEFT JOIN albums a ON t.album_id = a.id WHERE t.album_id IS NOT NULL AND a.id IS NULL
UNION ALL SELECT 'likes_invalid_user', COUNT(*) FROM likes l LEFT JOIN users u ON l.user_id = u.id WHERE u.id IS NULL
UNION ALL SELECT 'likes_invalid_track', COUNT(*) FROM likes l LEFT JOIN tracks t ON l.target_id = t.id WHERE l.target_type = 'TRACK' AND t.id IS NULL
UNION ALL SELECT 'follows_invalid_artist', COUNT(*) FROM follows f LEFT JOIN artists a ON f.artist_id = a.id WHERE a.id IS NULL
UNION ALL SELECT 'follows_invalid_user', COUNT(*) FROM follows f LEFT JOIN users u ON f.user_id = u.id WHERE u.id IS NULL
UNION ALL SELECT 'playbacks_invalid_playlist', COUNT(*) FROM playbacks p LEFT JOIN playlists pl ON p.playlist_id = pl.id WHERE pl.id IS NULL
UNION ALL SELECT 'playbacks_invalid_track', COUNT(*) FROM playbacks p LEFT JOIN tracks t ON p.track_id = t.id WHERE t.id IS NULL
UNION ALL SELECT 'playbacks_invalid_user', COUNT(*) FROM playbacks p LEFT JOIN users u ON p.user_id = u.id WHERE u.id IS NULL
UNION ALL SELECT 'search_histories_invalid_user', COUNT(*) FROM search_histories sh LEFT JOIN users u ON sh.user_id = u.id WHERE sh.user_id IS NOT NULL AND u.id IS NULL
UNION ALL SELECT 'playlists_invalid_user', COUNT(*) FROM playlists p LEFT JOIN users u ON p.user_id = u.id WHERE u.id IS NULL
UNION ALL SELECT 'playlist_tracks_invalid_playlist', COUNT(*) FROM playlist_tracks pt LEFT JOIN playlists p ON pt.playlist_id = p.id WHERE p.id IS NULL
UNION ALL SELECT 'playlist_tracks_invalid_track', COUNT(*) FROM playlist_tracks pt LEFT JOIN tracks t ON pt.track_id = t.id WHERE t.id IS NULL
UNION ALL SELECT 'track_comments_invalid_user', COUNT(*) FROM track_comments tc LEFT JOIN users u ON tc.user_id = u.id WHERE u.id IS NULL
UNION ALL SELECT 'track_comments_invalid_track', COUNT(*) FROM track_comments tc LEFT JOIN tracks t ON tc.track_id = t.id WHERE t.id IS NULL
UNION ALL SELECT 'artist_applications_invalid_requester', COUNT(*) FROM artist_applications aa LEFT JOIN users u ON aa.requester_user_id = u.id WHERE u.id IS NULL
UNION ALL SELECT 'artist_applications_invalid_reviewer', COUNT(*) FROM artist_applications aa LEFT JOIN users u ON aa.reviewed_by_admin_id = u.id WHERE aa.reviewed_by_admin_id IS NOT NULL AND u.id IS NULL
UNION ALL SELECT 'album_applications_invalid_requester', COUNT(*) FROM album_applications aa LEFT JOIN users u ON aa.requester_user_id = u.id WHERE u.id IS NULL
UNION ALL SELECT 'album_applications_invalid_artist', COUNT(*) FROM album_applications aa LEFT JOIN artists a ON aa.artist_id = a.id WHERE a.id IS NULL
UNION ALL SELECT 'album_applications_invalid_reviewer', COUNT(*) FROM album_applications aa LEFT JOIN users u ON aa.reviewed_by_admin_id = u.id WHERE aa.reviewed_by_admin_id IS NOT NULL AND u.id IS NULL
UNION ALL SELECT 'track_applications_invalid_requester', COUNT(*) FROM track_applications ta LEFT JOIN users u ON ta.requester_user_id = u.id WHERE u.id IS NULL
UNION ALL SELECT 'track_applications_invalid_artist', COUNT(*) FROM track_applications ta LEFT JOIN artists a ON ta.artist_id = a.id WHERE ta.artist_id IS NOT NULL AND a.id IS NULL
UNION ALL SELECT 'track_applications_invalid_album', COUNT(*) FROM track_applications ta LEFT JOIN albums a ON ta.album_id = a.id WHERE ta.album_id IS NOT NULL AND a.id IS NULL
UNION ALL SELECT 'track_applications_invalid_reviewer', COUNT(*) FROM track_applications ta LEFT JOIN users u ON ta.reviewed_by_admin_id = u.id WHERE ta.reviewed_by_admin_id IS NOT NULL AND u.id IS NULL;


SELECT '==================== DOMAIN POLICY CHECK ====================' AS section;

SELECT 'tracks_official_release_album_artist_mismatch' AS check_name, COUNT(*) AS invalid_count
FROM tracks t
         JOIN albums a ON t.album_id = a.id
WHERE t.track_type = 'OFFICIAL_RELEASE'
  AND t.artist_id <> a.artist_id

UNION ALL SELECT 'tracks_free_creation_invalid_fields', COUNT(*)
FROM tracks t
WHERE t.track_type = 'FREE_CREATION'
  AND (
    t.artist_id IS NOT NULL
        OR t.album_id IS NOT NULL
        OR t.track_number IS NOT NULL
        OR t.featured_artist_text IS NOT NULL
    )

UNION ALL SELECT 'tracks_official_release_missing_fields', COUNT(*)
FROM tracks t
WHERE t.track_type = 'OFFICIAL_RELEASE'
  AND (
    t.artist_id IS NULL
        OR t.album_id IS NULL
        OR t.track_number IS NULL
    )

UNION ALL SELECT 'tracks_invalid_audio_key_format', COUNT(*)
FROM tracks t
WHERE t.audio_key IS NULL
   OR t.audio_key = ''
   OR t.audio_key LIKE 'http://%'
   OR t.audio_key LIKE 'https://%'
   OR t.audio_key NOT LIKE 'tracks/audio/%.mp3'

UNION ALL SELECT 'track_applications_official_release_album_artist_mismatch', COUNT(*)
FROM track_applications ta
         JOIN albums a ON ta.album_id = a.id
WHERE ta.track_type = 'OFFICIAL_RELEASE'
  AND ta.artist_id <> a.artist_id

UNION ALL SELECT 'track_applications_free_creation_invalid_fields', COUNT(*)
FROM track_applications ta
WHERE ta.track_type = 'FREE_CREATION'
  AND (
    ta.artist_id IS NOT NULL
        OR ta.album_id IS NOT NULL
        OR ta.track_number IS NOT NULL
        OR ta.featured_artist_text IS NOT NULL
        OR ta.publish_delay_days IS NOT NULL
    )

UNION ALL SELECT 'track_applications_official_release_missing_fields', COUNT(*)
FROM track_applications ta
WHERE ta.track_type = 'OFFICIAL_RELEASE'
  AND (
    ta.artist_id IS NULL
        OR ta.album_id IS NULL
        OR ta.track_number IS NULL
        OR ta.publish_delay_days IS NULL
    )

UNION ALL SELECT 'track_applications_invalid_audio_key_format', COUNT(*)
FROM track_applications ta
WHERE ta.audio_key IS NULL
   OR ta.audio_key = ''
   OR ta.audio_key LIKE 'http://%'
   OR ta.audio_key LIKE 'https://%'
   OR ta.audio_key NOT LIKE 'tracks/audio/%.mp3'

UNION ALL SELECT 'track_applications_pending_invalid_review_fields', COUNT(*)
FROM track_applications ta
WHERE ta.status = 'PENDING'
  AND (
    ta.reviewed_by_admin_id IS NOT NULL
        OR ta.reviewed_at IS NOT NULL
        OR ta.rejection_reason IS NOT NULL
    )

UNION ALL SELECT 'track_applications_approved_invalid_review_fields', COUNT(*)
FROM track_applications ta
WHERE ta.status = 'APPROVED'
  AND (
    ta.reviewed_by_admin_id IS NULL
        OR ta.reviewed_at IS NULL
        OR ta.rejection_reason IS NOT NULL
    )

UNION ALL SELECT 'track_applications_rejected_invalid_review_fields', COUNT(*)
FROM track_applications ta
WHERE ta.status = 'REJECTED'
  AND (
    ta.reviewed_by_admin_id IS NULL
        OR ta.reviewed_at IS NULL
        OR ta.rejection_reason IS NULL
    );
