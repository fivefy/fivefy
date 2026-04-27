SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE track_comments;
TRUNCATE TABLE track_applications;
TRUNCATE TABLE album_applications;
TRUNCATE TABLE artist_applications;
TRUNCATE TABLE tracks;
TRUNCATE TABLE albums;
TRUNCATE TABLE artists;
TRUNCATE TABLE subscriptions;
TRUNCATE TABLE payments;
TRUNCATE TABLE point_orders;
TRUNCATE TABLE cash_orders;
TRUNCATE TABLE billing_keys;
TRUNCATE TABLE point_histories;
TRUNCATE TABLE wallets;
TRUNCATE TABLE users;

SET FOREIGN_KEY_CHECKS = 1;
