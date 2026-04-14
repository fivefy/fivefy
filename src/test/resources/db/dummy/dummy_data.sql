-- ================================================================
-- 음악 구독 서비스 더미데이터
-- 엔티티 최신화 반영 (영문 테이블명, ENUM, 컬럼명 변경)

-- ⚠️  경고: 이 스크립트는 로컬 개발 전용입니다.
--          stage / prod DB에서는 절대 실행하지 마세요.

-- [WARNING] 본 스크립트는 MySQL 8.0 이상의 로컬 개발 환경 전용입니다.
-- H2 및 자동화 테스트 환경(Spring Test)에서는 실행되지 않으며, 포함되어서도 안 됩니다.

-- ================================================================

-- ================================================================
-- 로컬 DB 가드: DB명이 'fivefy_db'가 아니면 중단
-- ================================================================

DELIMITER $$
DROP PROCEDURE IF EXISTS check_db_name $$
CREATE PROCEDURE check_db_name()
BEGIN
    IF DATABASE() != 'fivefy_db' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'ABORT: 로컬 DB(fivefy_db)에서만 실행 가능합니다.';
    END IF;
END $$
DELIMITER ;
-- 프로시저 실행
CALL check_db_name();
-- 실행 후 프로시저 삭제 (흔적 남기지 않음)
DROP PROCEDURE IF EXISTS check_db_name;

DELIMITER $$
DROP PROCEDURE IF EXISTS check_safety_environment $$
CREATE PROCEDURE check_safety_environment()
BEGIN
    DECLARE is_rds INT DEFAULT 0;
    -- 1. RDS 호스트네임 체크
    IF @@hostname LIKE '%.rds.amazonaws.com' THEN
        SET is_rds = 1;
    END IF;
    -- 2. RDS이거나 DB 이름이 운영(fivefy_db가 아님)인 경우 에러 발생
    IF is_rds = 1 OR DATABASE() != 'fivefy_db' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '⚠️ [ACCESS DENIED] 운영 환경 또는 RDS에서 더미 데이터 실행이 차단되었습니다.';
    END IF;
END $$
DELIMITER ;
-- 검사 실행
CALL check_safety_environment();
-- 검사 통과 시에만 아래 로직이 실행됨
DROP PROCEDURE IF EXISTS check_safety_environment;

-- ================================================================
-- 트랜잭션 시작 (중간 실패 시 전체 롤백 보장)
-- ================================================================
START TRANSACTION;

SET FOREIGN_KEY_CHECKS = 0;
SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;

-- ================================================================
-- 1. users (110명)
-- ================================================================
TRUNCATE TABLE users;

-- 비밀번호 : test1234!
INSERT INTO users (id, email, password, name, role, status, last_active_at, created_at, updated_at, deleted_at) VALUES
-- 관리자 5명
(1,  'admin1@music.com', '$2a$10$8u9Y7uVlU9G.XkX0Z3WlTeR7XjG2K5S7v0o4B/6fK6A7Uv8y5C6yS', '관리자1', 'ADMIN', 'ACTIVE', NOW(), '2024-06-01 09:00:00', NOW(), NULL),
(2,  'admin2@music.com', '$2a$10$8u9Y7uVlU9G.XkX0Z3WlTeR7XjG2K5S7v0o4B/6fK6A7Uv8y5C6yS', '관리자2', 'ADMIN', 'ACTIVE', NOW(), '2024-06-01 09:00:00', NOW(), NULL),
(3,  'admin3@music.com', '$2a$10$8u9Y7uVlU9G.XkX0Z3WlTeR7XjG2K5S7v0o4B/6fK6A7Uv8y5C6yS', '관리자3', 'ADMIN', 'ACTIVE', NOW(), '2024-06-01 09:00:00', NOW(), NULL),
(4,  'admin4@music.com', '$2a$10$8u9Y7uVlU9G.XkX0Z3WlTeR7XjG2K5S7v0o4B/6fK6A7Uv8y5C6yS', '관리자4', 'ADMIN', 'ACTIVE', NOW(), '2024-06-01 09:00:00', NOW(), NULL),
(5,  'admin5@music.com', '$2a$10$8u9Y7uVlU9G.XkX0Z3WlTeR7XjG2K5S7v0o4B/6fK6A7Uv8y5C6yS', '관리자5', 'ADMIN', 'ACTIVE', NOW(), '2024-06-01 09:00:00', NOW(), NULL),
-- 아티스트 계정 5명
(6,  'artist_luna@music.com',  '$2a$10$8u9Y7uVlU9G.XkX0Z3WlTeR7XjG2K5S7v0o4B/6fK6A7Uv8y5C6yS', '루나',  'USER', 'ACTIVE', NOW(), '2024-07-01 10:00:00', NOW(), NULL),
(7,  'artist_nova@music.com',  '$2a$10$8u9Y7uVlU9G.XkX0Z3WlTeR7XjG2K5S7v0o4B/6fK6A7Uv8y5C6yS', '노바',  'USER', 'ACTIVE', NOW(), '2024-07-02 10:00:00', NOW(), NULL),
(8,  'artist_echo@music.com',  '$2a$10$8u9Y7uVlU9G.XkX0Z3WlTeR7XjG2K5S7v0o4B/6fK6A7Uv8y5C6yS', '에코',  'USER', 'ACTIVE', NOW(), '2024-07-03 10:00:00', NOW(), NULL),
(9,  'artist_dawn@music.com',  '$2a$10$8u9Y7uVlU9G.XkX0Z3WlTeR7XjG2K5S7v0o4B/6fK6A7Uv8y5C6yS', '던',    'USER', 'ACTIVE', NOW(), '2024-07-04 10:00:00', NOW(), NULL),
(10, 'artist_pulse@music.com', '$2a$10$8u9Y7uVlU9G.XkX0Z3WlTeR7XjG2K5S7v0o4B/6fK6A7Uv8y5C6yS', '펄스',  'USER', 'ACTIVE', NOW(), '2024-07-05 10:00:00', NOW(), NULL),
-- SUSPENDED (30일 미접속)
(11, 'inactive1@music.com', '$2a$10$8u9Y7uVlU9G.XkX0Z3WlTeR7XjG2K5S7v0o4B/6fK6A7Uv8y5C6yS', '비활성1', 'USER', 'SUSPENDED', DATE_SUB(NOW(), INTERVAL 35 DAY), '2024-08-01 10:00:00', NOW(), NULL),
(12, 'inactive2@music.com', '$2a$10$8u9Y7uVlU9G.XkX0Z3WlTeR7XjG2K5S7v0o4B/6fK6A7Uv8y5C6yS', '비활성2', 'USER', 'SUSPENDED', DATE_SUB(NOW(), INTERVAL 40 DAY), '2024-08-02 10:00:00', NOW(), NULL),
-- DELETED (소프트 삭제)
(13, 'deleted1@music.com', '$2a$10$8u9Y7uVlU9G.XkX0Z3WlTeR7XjG2K5S7v0o4B/6fK6A7Uv8y5C6yS', '탈퇴유저1', 'USER', 'DELETED', '2025-10-04 10:00:00', '2024-09-01 10:00:00', NOW(), '2025-10-04 10:00:00'),
(14, 'deleted2@music.com', '$2a$10$8u9Y7uVlU9G.XkX0Z3WlTeR7XjG2K5S7v0o4B/6fK6A7Uv8y5C6yS', '탈퇴유저2', 'USER', 'DELETED', '2025-07-26 10:00:00', '2024-09-02 10:00:00', NOW(), '2025-07-26 10:00:00');

-- 일반 유저 96명 (id 15~110)
DELIMITER $$
DROP PROCEDURE IF EXISTS insert_users $$
CREATE PROCEDURE insert_users()
BEGIN
    DECLARE i INT DEFAULT 15;
    WHILE i <= 110 DO
            INSERT INTO users (id, email, password, name, role, status, last_active_at, created_at, updated_at, deleted_at)
            VALUES (
                       i,
                       CONCAT('user', i, '@music.com'),
                       '$2a$10$8u9Y7uVlU9G.XkX0Z3WlTeR7XjG2K5S7v0o4B/6fK6A7Uv8y5C6yS',
                       CONCAT('유저', i),
                       'USER',
                       'ACTIVE',
                       DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 25) DAY),
                       DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 180 + 30) DAY),
                       NOW(),
                       NULL
                   );
            SET i = i + 1;
        END WHILE;
END $$
DELIMITER ;
CALL insert_users();
DROP PROCEDURE IF EXISTS insert_users;

-- ================================================================
-- 2. artist_applications (승인 5 + 대기 2 + 반려 1)
-- ================================================================
TRUNCATE TABLE artist_applications;

INSERT INTO artist_applications
(id, requester_user_id, requested_name, bio, profile_image_url, status,
 reviewed_by_admin_id, reviewed_at, rejection_reason, created_at, updated_at)
VALUES
(1, 6,  '루나',     '일렉트로닉 팝 아티스트.',  'https://cdn.music.com/artists/luna.jpg',  'APPROVED', 1, '2024-07-05 11:00:00', NULL,                          '2024-07-01 10:00:00', '2024-07-05 11:00:00'),
(2, 7,  '노바',     'R&B 소울 가수.',           'https://cdn.music.com/artists/nova.jpg',  'APPROVED', 1, '2024-07-06 11:00:00', NULL,                          '2024-07-02 10:00:00', '2024-07-06 11:00:00'),
(3, 8,  '에코',     '인디 포크 밴드 리더.',      'https://cdn.music.com/artists/echo.jpg',  'APPROVED', 2, '2024-07-07 11:00:00', NULL,                          '2024-07-03 10:00:00', '2024-07-07 11:00:00'),
(4, 9,  '던',       '힙합 아티스트.',            'https://cdn.music.com/artists/dawn.jpg',  'APPROVED', 2, '2024-07-08 11:00:00', NULL,                          '2024-07-04 10:00:00', '2024-07-08 11:00:00'),
(5, 10, '펄스',     'EDM 프로듀서.',             'https://cdn.music.com/artists/pulse.jpg', 'APPROVED', 3, '2024-07-09 11:00:00', NULL,                          '2024-07-05 10:00:00', '2024-07-09 11:00:00'),
(6, 50, '라일락',   '클래식 크로스오버 가수.',   NULL,                                      'PENDING',  NULL, NULL,             NULL,                          '2025-01-10 10:00:00', '2025-01-10 10:00:00'),
(7, 51, '스톰',     '메탈 밴드 기타리스트.',     NULL,                                      'PENDING',  NULL, NULL,             NULL,                          '2025-01-12 10:00:00', '2025-01-12 10:00:00'),
(8, 52, '가짜가수', '관련 자료 없음.',           NULL,                                      'REJECTED', 1, '2024-12-20 11:00:00', '아티스트 활동 이력 확인 불가', '2024-12-18 10:00:00', '2024-12-20 11:00:00');

-- ================================================================
-- 3. artists (5명 — ACTIVE/SUSPENDED)
-- ================================================================
TRUNCATE TABLE artists;

INSERT INTO artists
(id, owner_user_id, name, bio, profile_image_url, status, created_at, updated_at, deleted_at)
VALUES
(1, 6,  '루나',  '일렉트로닉 팝 아티스트.',  'https://cdn.music.com/artists/luna.jpg',  'ACTIVE',    '2024-07-05 11:00:00', NOW(), NULL),
(2, 7,  '노바',  'R&B 소울 가수.',           'https://cdn.music.com/artists/nova.jpg',  'ACTIVE',    '2024-07-06 11:00:00', NOW(), NULL),
(3, 8,  '에코',  '인디 포크 밴드 리더.',      'https://cdn.music.com/artists/echo.jpg',  'ACTIVE',    '2024-07-07 11:00:00', NOW(), NULL),
(4, 9,  '던',    '힙합 아티스트.',            'https://cdn.music.com/artists/dawn.jpg',  'ACTIVE',    '2024-07-08 11:00:00', NOW(), NULL),
(5, 10, '펄스',  'EDM 프로듀서.',             'https://cdn.music.com/artists/pulse.jpg', 'SUSPENDED', '2024-07-09 11:00:00', NOW(), NULL);

-- ================================================================
-- 4. album_release_requests
-- ================================================================
TRUNCATE TABLE album_release_requests;

INSERT INTO album_release_requests
(id, requester_user_id, artist_id, title, description, cover_image_url,
 release_at, scheduled_publish_at, status, reviewed_by_admin_id, reviewed_at,
 rejection_reason, created_at, updated_at)
VALUES
(1, 6, 1, 'Midnight Dreams', '루나의 첫 번째 정규 앨범.', 'https://cdn.music.com/albums/midnight.jpg', '2024-08-01 00:00:00', '2024-08-01 00:00:00', 'APPROVED', 1, '2024-07-25 10:00:00', NULL, '2024-07-20 10:00:00', '2024-07-25 10:00:00'),
(2, 6, 1, 'Neon Echoes',     '루나의 두 번째 앨범.',      'https://cdn.music.com/albums/neon.jpg',     '2024-11-01 00:00:00', '2024-11-01 00:00:00', 'APPROVED', 1, '2024-10-25 10:00:00', NULL, '2024-10-20 10:00:00', '2024-10-25 10:00:00'),
(3, 7, 2, 'Soul Waves',      '노바의 데뷔 앨범.',          'https://cdn.music.com/albums/soul.jpg',     '2024-08-15 00:00:00', '2024-08-15 00:00:00', 'APPROVED', 2, '2024-08-10 10:00:00', NULL, '2024-08-05 10:00:00', '2024-08-10 10:00:00'),
(4, 7, 2, 'Warm Silence',    '노바의 두 번째 앨범.',       'https://cdn.music.com/albums/warm.jpg',     '2025-01-01 00:00:00', '2025-01-01 00:00:00', 'APPROVED', 2, '2024-12-25 10:00:00', NULL, '2024-12-20 10:00:00', '2024-12-25 10:00:00'),
(5, 8, 3, 'Forest Tales',    '에코의 첫 앨범.',             'https://cdn.music.com/albums/forest.jpg',  '2024-09-01 00:00:00', '2024-09-01 00:00:00', 'APPROVED', 3, '2024-08-25 10:00:00', NULL, '2024-08-20 10:00:00', '2024-08-25 10:00:00'),
(6, 9, 4, 'City Bars',       '던의 데뷔 힙합 앨범.',       'https://cdn.music.com/albums/city.jpg',     '2024-09-15 00:00:00', '2024-09-15 00:00:00', 'APPROVED', 1, '2024-09-10 10:00:00', NULL, '2024-09-05 10:00:00', '2024-09-10 10:00:00'),
(7, 10,5, 'Energy Field',    '펄스의 EDM 앨범.',            'https://cdn.music.com/albums/energy.jpg',  '2024-10-01 00:00:00', '2024-10-01 00:00:00', 'APPROVED', 2, '2024-09-25 10:00:00', NULL, '2024-09-20 10:00:00', '2024-09-25 10:00:00');

-- ================================================================
-- 5. albums (9개)
-- ================================================================
TRUNCATE TABLE albums;

INSERT INTO albums
(id, artist_id, title, description, cover_image_url, release_at, status,
 scheduled_publish_at, published_at, track_count, total_duration_sec, created_at, updated_at, deleted_at)
VALUES
(1, 1, 'Midnight Dreams', '루나의 첫 번째 정규 앨범', 'https://cdn.music.com/albums/midnight.jpg', '2024-08-01 00:00:00', 'PUBLISHED',   '2024-08-01 00:00:00', '2024-08-01 00:00:00', 6, 1440, '2024-07-25 10:00:00', NOW(), NULL),
(2, 1, 'Neon Echoes',     '루나의 두 번째 앨범',       'https://cdn.music.com/albums/neon.jpg',     '2024-11-01 00:00:00', 'PUBLISHED',   '2024-11-01 00:00:00', '2024-11-01 00:00:00', 5, 1200, '2024-10-25 10:00:00', NOW(), NULL),
(3, 2, 'Soul Waves',      '노바의 데뷔 앨범',           'https://cdn.music.com/albums/soul.jpg',     '2024-08-15 00:00:00', 'PUBLISHED',   '2024-08-15 00:00:00', '2024-08-15 00:00:00', 6, 1500, '2024-08-10 10:00:00', NOW(), NULL),
(4, 2, 'Warm Silence',    '노바의 두 번째 앨범',        'https://cdn.music.com/albums/warm.jpg',     '2025-01-01 00:00:00', 'PUBLISHED',   '2025-01-01 00:00:00', '2025-01-01 00:00:00', 5, 1260, '2024-12-25 10:00:00', NOW(), NULL),
(5, 3, 'Forest Tales',    '에코의 첫 앨범',             'https://cdn.music.com/albums/forest.jpg',  '2024-09-01 00:00:00', 'PUBLISHED',   '2024-09-01 00:00:00', '2024-09-01 00:00:00', 6, 1380, '2024-08-25 10:00:00', NOW(), NULL),
(6, 4, 'City Bars',       '던의 데뷔 힙합 앨범',        'https://cdn.music.com/albums/city.jpg',     '2024-09-15 00:00:00', 'PUBLISHED',   '2024-09-15 00:00:00', '2024-09-15 00:00:00', 6, 1320, '2024-09-10 10:00:00', NOW(), NULL),
(7, 5, 'Energy Field',    '펄스의 EDM 앨범',            'https://cdn.music.com/albums/energy.jpg',  '2024-10-01 00:00:00', 'PUBLISHED',   '2024-10-01 00:00:00', '2024-10-01 00:00:00', 6, 1440, '2024-09-25 10:00:00', NOW(), NULL),
(8, 1, 'Aurora',          '루나의 세 번째 앨범 예약',   NULL,                                        '2025-03-01 00:00:00', 'UNPUBLISHED', '2025-03-01 00:00:00', NULL,                  0, 0,    '2025-01-15 10:00:00', NOW(), NULL),
(9, 3, 'Hidden Tracks',   '저작권 이슈로 블록됨',       NULL,                                        '2024-12-01 00:00:00', 'BLOCKED',     NULL,                  NULL,                  3, 720,  '2024-11-20 10:00:00', NOW(), NULL);

-- ================================================================
-- 6. track_release_requests
-- ================================================================
TRUNCATE TABLE track_release_requests;

INSERT INTO track_release_requests
(id, requester_user_id, track_type, artist_id, album_id, track_number, title, lyrics,
 genre, audio_url, duration_sec, featured_artist_text, scheduled_publish_at,
 status, reviewed_by_admin_id, reviewed_at, rejection_reason, created_at, updated_at)
VALUES
(1, 6, 'OFFICIAL_RELEASE', 1, 1, 1, 'Moonlight Drive', NULL, 'ELECTRONIC', 'https://audio.music.com/req/1.mp3', 234, NULL, NULL, 'APPROVED', 1, '2024-07-26 10:00:00', NULL, '2024-07-25 12:00:00', '2024-07-26 10:00:00'),
(2, 6, 'OFFICIAL_RELEASE', 1, 1, 2, 'Starfall',        NULL, 'ELECTRONIC', 'https://audio.music.com/req/2.mp3', 198, NULL, NULL, 'APPROVED', 1, '2024-07-26 10:00:00', NULL, '2024-07-25 12:00:00', '2024-07-26 10:00:00'),
(3, 7, 'OFFICIAL_RELEASE', 2, 3, 1, 'Ocean Soul',      NULL, 'RNB',        'https://audio.music.com/req/3.mp3', 245, NULL, NULL, 'APPROVED', 2, '2024-08-11 10:00:00', NULL, '2024-08-10 12:00:00', '2024-08-11 10:00:00'),
(4, 6, 'OFFICIAL_RELEASE', 1, 8, 1, 'Aurora Borealis', NULL, 'ELECTRONIC', 'https://audio.music.com/req/4.mp3', 280, NULL, '2025-03-01 00:00:00', 'PENDING', NULL, NULL, NULL, '2025-01-16 10:00:00', '2025-01-16 10:00:00');

-- ================================================================
-- 7. tracks (53개)
-- ================================================================
TRUNCATE TABLE tracks;

-- 앨범 1 (루나 - Midnight Dreams) 6트랙
INSERT INTO tracks
(id, owner_user_id, track_type, artist_id, album_id, track_number, title, lyrics, genre,
 audio_url, duration_sec, featured_artist_text, status, scheduled_publish_at, published_at,
 play_count, created_at, updated_at, deleted_at)
VALUES
(1,  6, 'OFFICIAL_RELEASE', 1, 1, 1, 'Moonlight Drive',    '달빛 아래 달려가는 꿈속에서', 'ELECTRONIC', 'https://audio.music.com/1.mp3',  234, NULL, 'PUBLISHED', NULL, '2024-08-01 00:00:00', 15420, '2024-07-26 10:00:00', NOW(), NULL),
(2,  6, 'OFFICIAL_RELEASE', 1, 1, 2, 'Starfall',           '별이 지는 밤 우리의 이야기',  'ELECTRONIC', 'https://audio.music.com/2.mp3',  198, NULL, 'PUBLISHED', NULL, '2024-08-01 00:00:00', 12300, '2024-07-26 10:00:00', NOW(), NULL),
(3,  6, 'OFFICIAL_RELEASE', 1, 1, 3, 'Dream Sequence',     NULL,                          'ELECTRONIC', 'https://audio.music.com/3.mp3',  256, NULL, 'PUBLISHED', NULL, '2024-08-01 00:00:00', 9800,  '2024-07-26 10:00:00', NOW(), NULL),
(4,  6, 'OFFICIAL_RELEASE', 1, 1, 4, 'Neon Pulse',         NULL,                          'ELECTRONIC', 'https://audio.music.com/4.mp3',  245, NULL, 'PUBLISHED', NULL, '2024-08-01 00:00:00', 8200,  '2024-07-26 10:00:00', NOW(), NULL),
(5,  6, 'OFFICIAL_RELEASE', 1, 1, 5, 'Void Walker',        NULL,                          'ELECTRONIC', 'https://audio.music.com/5.mp3',  220, NULL, 'PUBLISHED', NULL, '2024-08-01 00:00:00', 7100,  '2024-07-26 10:00:00', NOW(), NULL),
(6,  6, 'OFFICIAL_RELEASE', 1, 1, 6, 'Celestial',          '하늘 위, 별들의 합창',         'ELECTRONIC', 'https://audio.music.com/6.mp3',  287, NULL, 'PUBLISHED', NULL, '2024-08-01 00:00:00', 11500, '2024-07-26 10:00:00', NOW(), NULL),
-- 앨범 2 (루나 - Neon Echoes) 5트랙
(7,  6, 'OFFICIAL_RELEASE', 1, 2, 1, 'City Light',         NULL, 'ELECTRONIC', 'https://audio.music.com/7.mp3',  232, NULL, 'PUBLISHED', NULL, '2024-11-01 00:00:00', 13400, '2024-10-25 10:00:00', NOW(), NULL),
(8,  6, 'OFFICIAL_RELEASE', 1, 2, 2, 'Neon Rain',          NULL, 'ELECTRONIC', 'https://audio.music.com/8.mp3',  245, NULL, 'PUBLISHED', NULL, '2024-11-01 00:00:00', 10200, '2024-10-25 10:00:00', NOW(), NULL),
(9,  6, 'OFFICIAL_RELEASE', 1, 2, 3, 'Echo Chamber',       NULL, 'ELECTRONIC', 'https://audio.music.com/9.mp3',  258, NULL, 'PUBLISHED', NULL, '2024-11-01 00:00:00', 8900,  '2024-10-25 10:00:00', NOW(), NULL),
(10, 6, 'OFFICIAL_RELEASE', 1, 2, 4, 'Signal Lost',        NULL, 'ELECTRONIC', 'https://audio.music.com/10.mp3', 215, NULL, 'PUBLISHED', NULL, '2024-11-01 00:00:00', 7600,  '2024-10-25 10:00:00', NOW(), NULL),
(11, 6, 'OFFICIAL_RELEASE', 1, 2, 5, 'Final Transmission', NULL, 'ELECTRONIC', 'https://audio.music.com/11.mp3', 270, NULL, 'PUBLISHED', NULL, '2024-11-01 00:00:00', 9300,  '2024-10-25 10:00:00', NOW(), NULL),
-- 앨범 3 (노바 - Soul Waves) 6트랙
(12, 7, 'OFFICIAL_RELEASE', 2, 3, 1, 'Ocean Soul',         '바다의 깊이만큼 깊은 마음', 'RNB', 'https://audio.music.com/12.mp3', 245, NULL, 'PUBLISHED', NULL, '2024-08-15 00:00:00', 18200, '2024-08-10 10:00:00', NOW(), NULL),
(13, 7, 'OFFICIAL_RELEASE', 2, 3, 2, 'Midnight Rain',      '한밤의 빗소리가 나를 감싸', 'RNB', 'https://audio.music.com/13.mp3', 220, NULL, 'PUBLISHED', NULL, '2024-08-15 00:00:00', 14600, '2024-08-10 10:00:00', NOW(), NULL),
(14, 7, 'OFFICIAL_RELEASE', 2, 3, 3, 'Velvet Whisper',     NULL, 'RNB', 'https://audio.music.com/14.mp3', 262, NULL, 'PUBLISHED', NULL, '2024-08-15 00:00:00', 11300, '2024-08-10 10:00:00', NOW(), NULL),
(15, 7, 'OFFICIAL_RELEASE', 2, 3, 4, 'Golden Hours',       NULL, 'RNB', 'https://audio.music.com/15.mp3', 248, NULL, 'PUBLISHED', NULL, '2024-08-15 00:00:00', 9800,  '2024-08-10 10:00:00', NOW(), NULL),
(16, 7, 'OFFICIAL_RELEASE', 2, 3, 5, 'Rhythm of You',      '당신의 리듬에 맞춰 춤을',   'RNB', 'https://audio.music.com/16.mp3', 256, NULL, 'PUBLISHED', NULL, '2024-08-15 00:00:00', 13400, '2024-08-10 10:00:00', NOW(), NULL),
(17, 7, 'OFFICIAL_RELEASE', 2, 3, 6, 'Fade to Blue',       NULL, 'RNB', 'https://audio.music.com/17.mp3', 269, NULL, 'PUBLISHED', NULL, '2024-08-15 00:00:00', 8700,  '2024-08-10 10:00:00', NOW(), NULL),
-- 앨범 4 (노바 - Warm Silence) 5트랙
(18, 7, 'OFFICIAL_RELEASE', 2, 4, 1, 'Still Water',  NULL, 'RNB', 'https://audio.music.com/18.mp3', 240, NULL, 'PUBLISHED', NULL, '2025-01-01 00:00:00', 5600, '2024-12-25 10:00:00', NOW(), NULL),
(19, 7, 'OFFICIAL_RELEASE', 2, 4, 2, 'Morning Dew',  NULL, 'RNB', 'https://audio.music.com/19.mp3', 255, NULL, 'PUBLISHED', NULL, '2025-01-01 00:00:00', 4800, '2024-12-25 10:00:00', NOW(), NULL),
(20, 7, 'OFFICIAL_RELEASE', 2, 4, 3, 'Gentle Storm', NULL, 'RNB', 'https://audio.music.com/20.mp3', 268, NULL, 'PUBLISHED', NULL, '2025-01-01 00:00:00', 4200, '2024-12-25 10:00:00', NOW(), NULL),
(21, 7, 'OFFICIAL_RELEASE', 2, 4, 4, 'Soft Thunder', NULL, 'RNB', 'https://audio.music.com/21.mp3', 242, NULL, 'PUBLISHED', NULL, '2025-01-01 00:00:00', 3900, '2024-12-25 10:00:00', NOW(), NULL),
(22, 7, 'OFFICIAL_RELEASE', 2, 4, 5, 'Lullaby',      NULL, 'RNB', 'https://audio.music.com/22.mp3', 255, NULL, 'PUBLISHED', NULL, '2025-01-01 00:00:00', 5100, '2024-12-25 10:00:00', NOW(), NULL),
-- 앨범 5 (에코 - Forest Tales) 6트랙
(23, 8, 'OFFICIAL_RELEASE', 3, 5, 1, 'Pine Road',    NULL, 'FOLK', 'https://audio.music.com/23.mp3', 218, NULL, 'PUBLISHED', NULL, '2024-09-01 00:00:00', 7200, '2024-08-25 10:00:00', NOW(), NULL),
(24, 8, 'OFFICIAL_RELEASE', 3, 5, 2, 'Maple Song',   '단풍잎이 떨어지는 계절', 'FOLK', 'https://audio.music.com/24.mp3', 232, NULL, 'PUBLISHED', NULL, '2024-09-01 00:00:00', 6800, '2024-08-25 10:00:00', NOW(), NULL),
(25, 8, 'OFFICIAL_RELEASE', 3, 5, 3, 'River Echo',   NULL, 'FOLK', 'https://audio.music.com/25.mp3', 245, NULL, 'PUBLISHED', NULL, '2024-09-01 00:00:00', 5600, '2024-08-25 10:00:00', NOW(), NULL),
(26, 8, 'OFFICIAL_RELEASE', 3, 5, 4, 'Stone Valley', NULL, 'FOLK', 'https://audio.music.com/26.mp3', 228, NULL, 'PUBLISHED', NULL, '2024-09-01 00:00:00', 4900, '2024-08-25 10:00:00', NOW(), NULL),
(27, 8, 'OFFICIAL_RELEASE', 3, 5, 5, 'Birch Forest', '자작나무 숲을 걷는 기억', 'FOLK', 'https://audio.music.com/27.mp3', 236, NULL, 'PUBLISHED', NULL, '2024-09-01 00:00:00', 5300, '2024-08-25 10:00:00', NOW(), NULL),
(28, 8, 'OFFICIAL_RELEASE', 3, 5, 6, 'Mountain Dusk',NULL, 'FOLK', 'https://audio.music.com/28.mp3', 221, NULL, 'PUBLISHED', NULL, '2024-09-01 00:00:00', 4700, '2024-08-25 10:00:00', NOW(), NULL),
-- 앨범 6 (던 - City Bars) 6트랙
(29, 9, 'OFFICIAL_RELEASE', 4, 6, 1, 'Seoul Night',       NULL, 'HIPHOP', 'https://audio.music.com/29.mp3', 198, NULL, 'PUBLISHED', NULL, '2024-09-15 00:00:00', 9400,  '2024-09-10 10:00:00', NOW(), NULL),
(30, 9, 'OFFICIAL_RELEASE', 4, 6, 2, 'Concrete Jungle',   NULL, 'HIPHOP', 'https://audio.music.com/30.mp3', 215, NULL, 'PUBLISHED', NULL, '2024-09-15 00:00:00', 8200,  '2024-09-10 10:00:00', NOW(), NULL),
(31, 9, 'OFFICIAL_RELEASE', 4, 6, 3, 'Flow State',        NULL, 'HIPHOP', 'https://audio.music.com/31.mp3', 224, NULL, 'PUBLISHED', NULL, '2024-09-15 00:00:00', 7600,  '2024-09-10 10:00:00', NOW(), NULL),
(32, 9, 'OFFICIAL_RELEASE', 4, 6, 4, 'Late Night Drive',  NULL, 'HIPHOP', 'https://audio.music.com/32.mp3', 232, NULL, 'PUBLISHED', NULL, '2024-09-15 00:00:00', 6900,  '2024-09-10 10:00:00', NOW(), NULL),
(33, 9, 'OFFICIAL_RELEASE', 4, 6, 5, 'Street Philosophy', NULL, 'HIPHOP', 'https://audio.music.com/33.mp3', 245, NULL, 'PUBLISHED', NULL, '2024-09-15 00:00:00', 8100,  '2024-09-10 10:00:00', NOW(), NULL),
(34, 9, 'OFFICIAL_RELEASE', 4, 6, 6, 'Outro: Dawn',       NULL, 'HIPHOP', 'https://audio.music.com/34.mp3', 206, NULL, 'PUBLISHED', NULL, '2024-09-15 00:00:00', 5800,  '2024-09-10 10:00:00', NOW(), NULL),
-- 앨범 7 (펄스 - Energy Field) 6트랙
(35, 10,'OFFICIAL_RELEASE', 5, 7, 1, 'Drop Zone',         NULL, 'EDM', 'https://audio.music.com/35.mp3', 198, NULL, 'PUBLISHED', NULL, '2024-10-01 00:00:00', 11200, '2024-09-25 10:00:00', NOW(), NULL),
(36, 10,'OFFICIAL_RELEASE', 5, 7, 2, 'Bass Rush',         NULL, 'EDM', 'https://audio.music.com/36.mp3', 215, NULL, 'PUBLISHED', NULL, '2024-10-01 00:00:00', 9800,  '2024-09-25 10:00:00', NOW(), NULL),
(37, 10,'OFFICIAL_RELEASE', 5, 7, 3, 'Voltage',           NULL, 'EDM', 'https://audio.music.com/37.mp3', 245, NULL, 'PUBLISHED', NULL, '2024-10-01 00:00:00', 8600,  '2024-09-25 10:00:00', NOW(), NULL),
(38, 10,'OFFICIAL_RELEASE', 5, 7, 4, 'Overdrive',         NULL, 'EDM', 'https://audio.music.com/38.mp3', 256, NULL, 'PUBLISHED', NULL, '2024-10-01 00:00:00', 7400,  '2024-09-25 10:00:00', NOW(), NULL),
(39, 10,'OFFICIAL_RELEASE', 5, 7, 5, 'Circuit Breaker',   NULL, 'EDM', 'https://audio.music.com/39.mp3', 232, NULL, 'PUBLISHED', NULL, '2024-10-01 00:00:00', 9100,  '2024-09-25 10:00:00', NOW(), NULL),
(40, 10,'OFFICIAL_RELEASE', 5, 7, 6, 'Power Down',        NULL, 'EDM', 'https://audio.music.com/40.mp3', 294, NULL, 'PUBLISHED', NULL, '2024-10-01 00:00:00', 6300,  '2024-09-25 10:00:00', NOW(), NULL),
-- FREE_CREATION 트랙 10개
(41, 20,'FREE_CREATION', NULL, NULL, NULL, '봄날의 기억',    '봄이 오면 생각나는 그 사람', 'INDIE',        'https://audio.music.com/41.mp3', 185, NULL, 'PUBLISHED', NULL, '2024-10-10 00:00:00', 2100, '2024-10-10 10:00:00', NOW(), NULL),
(42, 21,'FREE_CREATION', NULL, NULL, NULL, '여름밤',         NULL,                          'INDIE',        'https://audio.music.com/42.mp3', 195, NULL, 'PUBLISHED', NULL, '2024-10-15 00:00:00', 1800, '2024-10-15 10:00:00', NOW(), NULL),
(43, 22,'FREE_CREATION', NULL, NULL, NULL, 'Guitar Loop #1', NULL,                          'INSTRUMENTAL', 'https://audio.music.com/43.mp3', 210, NULL, 'PUBLISHED', NULL, '2024-11-01 00:00:00', 1500, '2024-11-01 10:00:00', NOW(), NULL),
(44, 23,'FREE_CREATION', NULL, NULL, NULL, 'Piano Sketch',   NULL,                          'INSTRUMENTAL', 'https://audio.music.com/44.mp3', 240, NULL, 'PUBLISHED', NULL, '2024-11-05 00:00:00', 1200, '2024-11-05 10:00:00', NOW(), NULL),
(45, 24,'FREE_CREATION', NULL, NULL, NULL, '카페에서',        '커피 한 잔 마시며',           'JAZZ',         'https://audio.music.com/45.mp3', 225, NULL, 'PUBLISHED', NULL, '2024-11-10 00:00:00', 900,  '2024-11-10 10:00:00', NOW(), NULL),
(46, 25,'FREE_CREATION', NULL, NULL, NULL, '산책',           NULL,                          'AMBIENT',      'https://audio.music.com/46.mp3', 300, NULL, 'PUBLISHED', NULL, '2024-11-15 00:00:00', 750,  '2024-11-15 10:00:00', NOW(), NULL),
(47, 26,'FREE_CREATION', NULL, NULL, NULL, 'Rainy Day',      NULL,                          'AMBIENT',      'https://audio.music.com/47.mp3', 312, NULL, 'PUBLISHED', NULL, '2024-11-20 00:00:00', 680,  '2024-11-20 10:00:00', NOW(), NULL),
(48, 27,'FREE_CREATION', NULL, NULL, NULL, '첫 눈',          '첫 눈이 내리는 날',           'INDIE',        'https://audio.music.com/48.mp3', 202, NULL, 'PUBLISHED', NULL, '2024-12-01 00:00:00', 1400, '2024-12-01 10:00:00', NOW(), NULL),
(49, 28,'FREE_CREATION', NULL, NULL, NULL, '별 헤는 밤',     NULL,                          'INDIE',        'https://audio.music.com/49.mp3', 218, NULL, 'PUBLISHED', NULL, '2024-12-10 00:00:00', 1100, '2024-12-10 10:00:00', NOW(), NULL),
(50, 29,'FREE_CREATION', NULL, NULL, NULL, 'Loop Study',     NULL,                          'ELECTRONIC',   'https://audio.music.com/50.mp3', 180, NULL, 'PUBLISHED', NULL, '2025-01-05 00:00:00', 890,  '2025-01-05 10:00:00', NOW(), NULL),
-- 앨범 8 (에코 - Hidden Tracks)
(51, 8,'OFFICIAL_RELEASE', 3, 9, 1, 'Blinding Lights',          '바람아 불어라',           'INDIE',        'https://audio.music.com/51.mp3', 210, NULL, 'BLOCKED', NULL, NULL, 0, '2024-11-20 10:00:00', NOW(), NULL),
(52, 8,'OFFICIAL_RELEASE', 3, 9, 2, 'Shape of You',     '조각같은 너',                          'INDIE',        'https://audio.music.com/52.mp3', 240, NULL, 'BLOCKED', NULL, NULL, 0, '2024-11-20 10:00:00', NOW(), NULL),
(53, 8,'OFFICIAL_RELEASE', 3, 9, 3, 'Yesterday',     '어제로 돌아가자',                          'ELECTRONIC',   'https://audio.music.com/53.mp3', 270, NULL, 'BLOCKED', NULL, NULL, 0,  '2024-11-20 10:00:00', NOW(), NULL);


-- ================================================================
-- 8. subscriptions (60건 — 기본 status: TRIAL)
-- ================================================================
TRUNCATE TABLE subscriptions;

DELIMITER $$
DROP PROCEDURE IF EXISTS insert_subscriptions $$
CREATE PROCEDURE insert_subscriptions()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE uid INT;
    DECLARE plan VARCHAR(10);
    DECLARE stat VARCHAR(10);
    DECLARE sdate DATETIME;
    DECLARE edate DATETIME;
    WHILE i <= 60 DO
            SET uid   = 15 + i;
            SET plan  = ELT(1 + FLOOR(RAND() * 3), 'MONTH', 'YEAR', 'FREE');
            SET stat  = ELT(1 + FLOOR(RAND() * 4), 'TRIAL', 'ACTIVE', 'EXPIRE', 'CANCELED');
            SET sdate = DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 180) DAY);
            SET edate = IF(plan='MONTH', DATE_ADD(sdate, INTERVAL 1 MONTH),
                           IF(plan='YEAR',  DATE_ADD(sdate, INTERVAL 1 YEAR),
                              DATE_ADD(sdate, INTERVAL 7 DAY)));
            INSERT INTO subscriptions (id, user_id, plan_type, status, start_date, expiry_date, next_billing_date, created_at)
            VALUES (i, uid, plan, stat, sdate, edate, IF(stat='ACTIVE', edate, NULL), sdate);
            SET i = i + 1;
        END WHILE;
END $$
DELIMITER ;
CALL insert_subscriptions();
DROP PROCEDURE IF EXISTS insert_subscriptions;

-- ================================================================
-- 9. wallets + point_histories
-- ================================================================
TRUNCATE TABLE wallets;
TRUNCATE TABLE point_histories;

DELIMITER $$
DROP PROCEDURE IF EXISTS insert_wallets $$
CREATE PROCEDURE insert_wallets()
BEGIN
    DECLARE i       INT    DEFAULT 1;
    DECLARE bal     BIGINT;
    DECLARE evt_bal BIGINT;
    DECLARE total   BIGINT;
    DECLARE hist_id BIGINT DEFAULT 1;

    WHILE i <= 110 DO
            SET bal     = FLOOR(RAND() * 50000);
            SET evt_bal = FLOOR(RAND() * 10000);
            SET total   = bal + evt_bal;

            INSERT INTO wallets (id, user_id, balance, event_balance, total_balance, created_at)
            VALUES (i, i, bal, evt_bal, total,
                    DATE_SUB(NOW(), INTERVAL FLOOR(RAND()*180) DAY));

            IF bal > 0 THEN
                INSERT INTO point_histories (id, point_id, pointtype, point_history_type, amount, balance_after, log_description, created_at)
                VALUES (hist_id, i, 'PAID', 'CHARGE', bal, bal, '포인트 충전',
                        DATE_SUB(NOW(), INTERVAL FLOOR(RAND()*60) DAY));
                SET hist_id = hist_id + 1;
            END IF;

            IF evt_bal > 0 THEN
                INSERT INTO point_histories (id, point_id, pointtype, point_history_type, amount, balance_after, log_description, created_at)
                VALUES (hist_id, i, 'FREE', 'CHARGE', evt_bal, total, '신규 가입 이벤트 포인트',
                        DATE_SUB(NOW(), INTERVAL FLOOR(RAND()*30) DAY));
                SET hist_id = hist_id + 1;
            END IF;

            SET i = i + 1;
        END WHILE;
END $$
DELIMITER ;
CALL insert_wallets();
DROP PROCEDURE IF EXISTS insert_wallets;

-- ================================================================
-- 10. playbacks (500건)
-- ================================================================
TRUNCATE TABLE playbacks;

DELIMITER $$
DROP PROCEDURE IF EXISTS insert_playbacks $$
CREATE PROCEDURE insert_playbacks()
BEGIN
    DECLARE i   INT DEFAULT 1;
    DECLARE uid BIGINT;
    DECLARE tid BIGINT;
    DECLARE dur INT;
    DECLARE track_dur INT;
    DECLARE stat VARCHAR(20);
    WHILE i <= 500 DO
            SET uid  = 15 + FLOOR(RAND() * 96);
            SET tid  = 1  + FLOOR(RAND() * 40);
            SELECT duration_sec INTO track_dur
            FROM tracks
            WHERE id = tid;
            SET stat = ELT(1 + FLOOR(RAND() * 5), 'COMPLETE', 'COMPLETE', 'COMPLETE', 'STOP', 'SKIP');
            SET dur  = IF(
                    stat = 'COMPLETE',
                    track_dur,
                    LEAST(track_dur, FLOOR(RAND() * track_dur) + 1)
                        );
            INSERT INTO playbacks (id, track_id, user_id, session_id, device_id, status, played_duration, played_at)
            VALUES (
                       i, tid, uid,
                       CONCAT('sess-', LPAD(FLOOR(RAND()*1000000), 7, '0')),
                       IF(RAND() > 0.1, CONCAT('dev-', FLOOR(RAND()*10)+1), NULL),
                       stat, dur,
                       DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 90) DAY)
                   );
            SET i = i + 1;
        END WHILE;
END $$
DELIMITER ;
CALL insert_playbacks();
DROP PROCEDURE IF EXISTS insert_playbacks;

-- ================================================================
-- 11. likes (300건 — UNIQUE: user_id + target_id + target_type)
-- ================================================================
TRUNCATE TABLE likes;

DELIMITER $$
DROP PROCEDURE IF EXISTS insert_likes $$
CREATE PROCEDURE insert_likes()
BEGIN
    DECLARE next_id  INT DEFAULT 1;
    DECLARE inserted INT DEFAULT 0;
    DECLARE uid   BIGINT;
    DECLARE tid   BIGINT;
    DECLARE ttype VARCHAR(10);
    WHILE inserted < 300 DO
            SET uid   = 15 + FLOOR(RAND() * 96);
            SET ttype = IF(RAND() < 0.8, 'TRACK', 'ALBUM');
            SET tid   = IF(ttype='TRACK', 1 + FLOOR(RAND()*40), 1 + FLOOR(RAND()*7));
            INSERT IGNORE INTO likes (id, user_id, target_id, target_type, created_at)
            VALUES (next_id, uid, tid, ttype, DATE_SUB(NOW(), INTERVAL FLOOR(RAND()*90) DAY));
            SET next_id = next_id + 1;
            IF ROW_COUNT() = 1 THEN
                SET inserted = inserted + 1;
            END IF;
        END WHILE;
END $$
DELIMITER ;
CALL insert_likes();
DROP PROCEDURE IF EXISTS insert_likes;

-- ================================================================
-- 12. track_comments (100건)
-- ================================================================
TRUNCATE TABLE track_comments;

INSERT INTO track_comments (id, user_id, track_id, content, status, created_at, updated_at, deleted_at) VALUES
(1,  20, 1,  '진짜 최고의 곡입니다. 루나 팬 영원히!',        'PUBLISHED', '2024-08-05 10:00:00', '2024-08-05 10:00:00', NULL),
(2,  21, 1,  '이 곡 들으면 항상 기분이 좋아져요',             'PUBLISHED', '2024-08-06 11:00:00', '2024-08-06 11:00:00', NULL),
(3,  22, 12, 'Ocean Soul 인생 곡이에요. 노바 최고!',          'PUBLISHED', '2024-08-20 12:00:00', '2024-08-20 12:00:00', NULL),
(4,  23, 12, '이 멜로디... 머릿속에서 계속 맴돌아요',         'PUBLISHED', '2024-08-21 09:00:00', '2024-08-21 09:00:00', NULL),
(5,  24, 29, '서울 밤거리 생각나는 곡. 던 진짜 천재.',        'PUBLISHED', '2024-09-20 15:00:00', '2024-09-20 15:00:00', NULL),
(6,  25, 35, '펄스 새 앨범 진짜 미쳤다.',                     'PUBLISHED', '2024-10-05 18:00:00', '2024-10-05 18:00:00', NULL),
(7,  26, 23, '에코 노래 들으면 숲 속에 있는 느낌.',           'PUBLISHED', '2024-09-10 10:00:00', '2024-09-10 10:00:00', NULL),
(8,  27, 2,  '루나 Starfall 들으면서 공부하면 집중 잘 됨.',   'PUBLISHED', '2024-08-15 20:00:00', '2024-08-15 20:00:00', NULL),
(9,  28, 13, '노바 목소리가 정말 특별해요.',                  'PUBLISHED', '2024-08-25 14:00:00', '2024-08-25 14:00:00', NULL),
(10, 29, 36, '이 베이스라인... 귀에서 안 떠나요.',            'PUBLISHED',   '2024-10-08 22:00:00', NOW(),                 '2025-01-10 09:00:00');

DELIMITER $$
DROP PROCEDURE IF EXISTS insert_more_comments $$
CREATE PROCEDURE insert_more_comments()
BEGIN
    DECLARE i        INT DEFAULT 11;
    DECLARE uid      BIGINT;
    DECLARE tid      BIGINT;
    DECLARE contents VARCHAR(255);
    WHILE i <= 100 DO
            SET uid      = 30 + FLOOR(RAND() * 80);
            SET tid      = 1  + FLOOR(RAND() * 40);
            SET contents = ELT(1 + FLOOR(RAND() * 5),
                               '좋은 음악 감사해요!', '이 곡 너무 좋아요', '자꾸 듣게 되네요',
                               '최고의 선택이에요',   '플레이리스트에 추가했어요');
            INSERT INTO track_comments (id, user_id, track_id, content, status, created_at, updated_at, deleted_at)
            VALUES (i, uid, tid, contents,'PUBLISHED',
                    DATE_SUB(NOW(), INTERVAL FLOOR(RAND()*90) DAY), NOW(), NULL);
            SET i = i + 1;
        END WHILE;
END $$
DELIMITER ;
CALL insert_more_comments();
DROP PROCEDURE IF EXISTS insert_more_comments;

-- ================================================================
-- 13. follows (150건 — UNIQUE: user_id + artist_id)
-- ================================================================
TRUNCATE TABLE follows;

DELIMITER $$
DROP PROCEDURE IF EXISTS insert_follows $$
CREATE PROCEDURE insert_follows()
BEGIN
    DECLARE next_id  INT DEFAULT 1;
    DECLARE inserted INT DEFAULT 0;
    WHILE inserted < 150 DO
            INSERT IGNORE INTO follows (id, artist_id, user_id, notification_enabled, created_at)
            VALUES (next_id,
                    1 + FLOOR(RAND() * 5),
                    15 + FLOOR(RAND() * 96),
                    RAND() < 0.7,
                    DATE_SUB(NOW(), INTERVAL FLOOR(RAND()*90) DAY));
            SET next_id = next_id + 1;
            IF ROW_COUNT() = 1 THEN
                SET inserted = inserted + 1;
            END IF;
        END WHILE;
END $$
DELIMITER ;
CALL insert_follows();
DROP PROCEDURE IF EXISTS insert_follows;

-- ================================================================
-- 14. orders + payments (10건 — ENUM status)
-- ================================================================
TRUNCATE TABLE orders;
TRUNCATE TABLE payments;

INSERT INTO orders (id, user_id, track_id, total_amount, order_number, status, created_at) VALUES
(1,  20, 1,  9900, 'ORD-20240901-0001', 'PENDING', '2024-09-01 10:00:00'),
(2,  21, 12, 9900, 'ORD-20240902-0002', 'PENDING', '2024-09-02 11:00:00'),
(3,  22, 29, 9900, 'ORD-20240903-0003', 'PENDING', '2024-09-03 12:00:00'),
(4,  23, 35, 9900, 'ORD-20240904-0004', 'PENDING', '2024-09-04 13:00:00'),
(5,  24, 7,  9900, 'ORD-20240905-0005', 'PENDING', '2024-09-05 14:00:00'),
(6,  25, 23, 9900, 'ORD-20240906-0006', 'PENDING', '2024-09-06 15:00:00'),
(7,  26, 13, 9900, 'ORD-20240907-0007', 'PENDING', '2024-09-07 16:00:00'),
(8,  27, 36, 9900, 'ORD-20240908-0008', 'PENDING', '2024-09-08 17:00:00'),
(9,  28, 2,  9900, 'ORD-20240909-0009', 'PENDING', '2024-09-09 18:00:00'),
(10, 29, 24, 9900, 'ORD-20240910-0010', 'PENDING', '2024-09-10 19:00:00');

INSERT INTO payments (id, order_id, amount, status, pg_transaction_id, idempotency_key, refund_reason, paid_at, refunded_at, created_at) VALUES
                                                                                                                                             (1,  1,  9900, 'REQUESTED', 'PG-TXN-001', 'IDEM-001', NULL,        NULL, NULL, '2024-09-01 10:00:00'),
                                                                                                                                             (2,  2,  9900, 'REQUESTED', 'PG-TXN-002', 'IDEM-002', NULL,        NULL, NULL, '2024-09-02 11:00:00'),
                                                                                                                                             (3,  3,  9900, 'REQUESTED', 'PG-TXN-003', 'IDEM-003', NULL,        NULL, NULL, '2024-09-03 12:00:00'),
                                                                                                                                             (4,  4,  9900, 'REQUESTED', 'PG-TXN-004', 'IDEM-004', NULL,        NULL, NULL, '2024-09-04 13:00:00'),
                                                                                                                                             (5,  5,  9900, 'REQUESTED', 'PG-TXN-005', 'IDEM-005', NULL,        NULL, NULL, '2024-09-05 14:00:00'),
                                                                                                                                             (6,  6,  9900, 'REQUESTED', 'PG-TXN-006', 'IDEM-006', NULL,        NULL, NULL, '2024-09-06 15:00:00'),
                                                                                                                                             (7,  7,  9900, 'REQUESTED', 'PG-TXN-007', 'IDEM-007', NULL,        NULL, NULL, '2024-09-07 16:00:00'),
                                                                                                                                             (8,  8,  9900, 'REQUESTED', 'PG-TXN-008', 'IDEM-008', NULL,        NULL, NULL, '2024-09-08 17:00:00'),
                                                                                                                                             (9,  9,  9900, 'REQUESTED', 'PG-TXN-009', 'IDEM-009', NULL,        NULL, NULL, '2024-09-09 18:00:00'),
                                                                                                                                             (10, 10, 9900, 'REQUESTED', 'PG-TXN-010', 'IDEM-010', NULL,        NULL, NULL, '2024-09-10 19:00:00');

-- ================================================================
-- 15. playlists + playlist_tracks (index → position)
-- ================================================================
TRUNCATE TABLE playlists;
TRUNCATE TABLE playlist_tracks;

INSERT INTO playlists (id, user_id, title, description, created_at, updated_at, deleted_at) VALUES
(1, 20, '밤에 듣는 음악',  '잠들기 전 감성 플리',    '2024-09-01 20:00:00', NOW(), NULL),
(2, 21, '운동할 때',       '에너지 넘치는 EDM 모음', '2024-09-10 07:00:00', NOW(), NULL),
(3, 22, '공부 집중 BGM',   '집중력 향상 앰비언트',   '2024-10-01 14:00:00', NOW(), NULL),
(4, 23, '인디 감성',       '감성 인디 모음집',       '2024-10-15 11:00:00', NOW(), NULL),
(5, 24, '힙합 장인',       '최고의 힙합 트랙 모음',  '2024-11-01 19:00:00', NOW(), NULL);

INSERT INTO playlist_tracks (id, playlist_id, track_id, position, created_at) VALUES
(1,  1, 1,  1, '2024-09-01 20:01:00'), (2,  1, 2,  2, '2024-09-01 20:02:00'),
(3,  1, 6,  3, '2024-09-01 20:03:00'), (4,  1, 12, 4, '2024-09-01 20:04:00'),
(5,  1, 13, 5, '2024-09-01 20:05:00'),
(6,  2, 35, 1, '2024-09-10 07:01:00'), (7,  2, 36, 2, '2024-09-10 07:02:00'),
(8,  2, 37, 3, '2024-09-10 07:03:00'), (9,  2, 38, 4, '2024-09-10 07:04:00'),
(10, 2, 39, 5, '2024-09-10 07:05:00'),
(11, 3, 46, 1, '2024-10-01 14:01:00'), (12, 3, 47, 2, '2024-10-01 14:02:00'),
(13, 4, 41, 1, '2024-10-15 11:01:00'), (14, 4, 48, 2, '2024-10-15 11:02:00'),
(15, 5, 29, 1, '2024-11-01 19:01:00'), (16, 5, 30, 2, '2024-11-01 19:02:00'),
(17, 5, 31, 3, '2024-11-01 19:03:00');

-- ================================================================
-- 16. popular_charts (Top 10 — chart_rank, snapshot_date)
-- ================================================================
TRUNCATE TABLE popular_charts;

INSERT INTO popular_charts (id, track_id, chart_rank, play_count, snapshot_date) VALUES
(1,  12, 1,  18200, '2025-01-13 00:00:00'),
(2,  1,  2,  15420, '2025-01-13 00:00:00'),
(3,  13, 3,  14600, '2025-01-13 00:00:00'),
(4,  7,  4,  13400, '2025-01-13 00:00:00'),
(5,  16, 5,  13400, '2025-01-13 00:00:00'),
(6,  2,  6,  12300, '2025-01-13 00:00:00'),
(7,  6, 7,  11500, '2025-01-13 00:00:00'),
(8,  35,  8,  11200, '2025-01-13 00:00:00'),
(9,  3, 9,   9800, '2025-01-13 00:00:00'),
(10, 11,  10,  9300, '2025-01-13 00:00:00');

-- ================================================================
-- 17. notifications
-- ================================================================
TRUNCATE TABLE notifications;

INSERT INTO notifications (id, user_id, type, content, status, channel, read_at, created_at) VALUES
(1, 20, 'PUBLISH_TRACK', '루나가 새 트랙 City Light을 발표했습니다.',       'SENT',   'IN_APP', '2024-11-01 09:00:00', '2024-11-01 00:05:00'),
(2, 21, 'TRACK_LIKED',   '회원님의 트랙에 좋아요가 눌렸습니다.',             'SENT',   'IN_APP', NULL,                  '2024-10-12 14:00:00'),
(3, 22, 'NEW_FOLLOWER',  '새로운 팔로워가 생겼습니다.',                      'SENT',   'IN_APP', '2024-09-05 10:00:00', '2024-09-05 08:00:00'),
(4, 23, 'SUBSCRIBE',     '구독이 갱신되었습니다.',                           'SENT',   'EMAIL',  NULL,                  '2024-10-01 00:00:00'),
(5, 6,  'ALBUM_LIKED',   '앨범 Midnight Dreams에 좋아요가 눌렸습니다.',      'SENT',   'IN_APP', '2024-09-02 12:00:00', '2024-09-02 11:00:00'),
(6, 30, 'COMMENT_REPLY', '회원님의 댓글에 답글이 달렸습니다.',               'QUEUED', 'IN_APP', NULL,                  NOW()),
(7, 31, 'NEW_FOLLOWER',  '새로운 팔로워가 생겼습니다.',                      'FAILED', 'PUSH',   NULL,                  NOW());

-- ================================================================
-- 18. search_histories
-- ================================================================
TRUNCATE TABLE search_histories;

INSERT INTO search_histories (id, user_id, keyword, result_count, created_at) VALUES
(1,  20, '루나',      24, '2025-01-10 10:00:00'),
(2,  20, 'Moonlight', 5,  '2025-01-10 10:05:00'),
(3,  21, '노바',      18, '2025-01-11 11:00:00'),
(4,  22, 'EDM',       40, '2025-01-12 12:00:00'),
(5,  23, '힙합',      32, '2025-01-13 13:00:00'),
(6,  24, 'indie',     15, '2025-01-14 14:00:00'),
(7,  25, 'piano',     8,  '2025-01-15 15:00:00'),
(8,  26, '에코',      12, '2025-01-15 16:00:00'),
(9,  27, 'soul',      20, '2025-01-16 09:00:00'),
(10, 28, 'midnight',  10, '2025-01-16 10:00:00');

-- ================================================================
-- 19. recommendations
-- ================================================================
TRUNCATE TABLE recommendations;

INSERT INTO recommendations (id, user_id, track_id, score, reason, created_at) VALUES
(1, 20, 12, 95, '재생 이력 기반 R&B 추천',        NOW()),
(2, 20, 13, 88, '좋아요 패턴 기반 추천',           NOW()),
(3, 21, 35, 92, '재생 이력 기반 EDM 추천',         NOW()),
(4, 22, 1,  85, '팔로우 아티스트 최신 트랙',       NOW()),
(5, 23, 29, 90, '재생 이력 기반 힙합 추천',        NOW());

-- ================================================================
-- 마무리
-- ================================================================
SET FOREIGN_KEY_CHECKS = 1;
SET UNIQUE_CHECKS = @OLD_UNIQUE_CHECKS;

-- 모든 INSERT 성공 시 커밋
-- 중간에 오류가 났다면 여기까지 도달하지 못하고 트랜잭션이 열린 채로 남음
-- → 해당 세션 종료 시 자동 롤백됨
COMMIT;

-- 데이터 확인
SELECT 'users'                AS 테이블, COUNT(*) AS 행수 FROM users
UNION ALL SELECT 'artists',              COUNT(*) FROM artists
UNION ALL SELECT 'albums',               COUNT(*) FROM albums
UNION ALL SELECT 'tracks',               COUNT(*) FROM tracks
UNION ALL SELECT 'subscriptions',        COUNT(*) FROM subscriptions
UNION ALL SELECT 'playbacks',            COUNT(*) FROM playbacks
UNION ALL SELECT 'likes',                COUNT(*) FROM likes
UNION ALL SELECT 'track_comments',       COUNT(*) FROM track_comments
UNION ALL SELECT 'follows',              COUNT(*) FROM follows
UNION ALL SELECT 'wallets',              COUNT(*) FROM wallets
UNION ALL SELECT 'point_histories',      COUNT(*) FROM point_histories
UNION ALL SELECT 'orders',               COUNT(*) FROM orders
UNION ALL SELECT 'payments',             COUNT(*) FROM payments
UNION ALL SELECT 'playlists',            COUNT(*) FROM playlists
UNION ALL SELECT 'playlist_tracks',      COUNT(*) FROM playlist_tracks
UNION ALL SELECT 'popular_charts',       COUNT(*) FROM popular_charts
UNION ALL SELECT 'notifications',        COUNT(*) FROM notifications
UNION ALL SELECT 'search_histories',     COUNT(*) FROM search_histories
UNION ALL SELECT 'recommendations',      COUNT(*) FROM recommendations
UNION ALL SELECT 'artist_applications',  COUNT(*) FROM artist_applications
UNION ALL SELECT 'album_release_requests', COUNT(*) FROM album_release_requests
UNION ALL SELECT 'track_release_requests', COUNT(*) FROM track_release_requests;
