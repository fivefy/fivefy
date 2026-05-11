-- 신청 도메인 중복 방지를 위한 DB 최종 방어선 추가
-- 주의:
-- - MySQL에서 STORED generated column 추가는 ALGORITHM=INSTANT 대상이 아니며,
--   데이터 규모에 따라 테이블 rebuild 및 DDL lock이 발생할 수 있다.
-- - 현재 프로젝트 seed / 로컬 검증 기준에서는 V8 적용을 확인했지만,
--   운영 대용량 track_applications 테이블에 반영할 경우 배포 전 row count 확인이 필요하다.
-- - 대량 데이터 환경에서는 낮은 트래픽 시간대 배포 또는 pt-online-schema-change / gh-ost 같은
--   online schema change 도구 사용을 검토한다.
-- - 본 마이그레이션은 application_id UNIQUE와 TrackApplication 정책별 UNIQUE를 한 번에 정리해
--   신청 row 중복 생성과 승인 결과 엔티티 중복 생성을 DB 레벨에서 차단하기 위한 변경이다.
--
-- 배포 전 사전 검증:
-- - UNIQUE INDEX 생성 시 기존 데이터에 중복 key가 있으면 ERROR 1062로 마이그레이션이 중단된다.
-- - 운영 DB 반영 전 아래 쿼리로 TrackApplication 정책별 중복 데이터를 먼저 확인한다.
-- - 중복이 발견되면 기존 중복 신청을 REJECTED 등 정책상 유효한 상태로 정리한 뒤 V8을 적용한다.
--
-- FREE_CREATION PENDING 중복 확인
/*
 SELECT
     requester_user_id,
     title,
     audio_url,
     COUNT(*) AS duplicate_count
 FROM track_applications
 WHERE track_type = 'FREE_CREATION'
   AND status = 'PENDING'
 GROUP BY requester_user_id, title, audio_url
 HAVING COUNT(*) > 1;
*/
--
-- OFFICIAL_RELEASE track_number 기준 중복 확인
/*
 SELECT
     requester_user_id,
     artist_id,
     album_id,
     track_number,
     COUNT(*) AS duplicate_count
 FROM track_applications
 WHERE track_type = 'OFFICIAL_RELEASE'
   AND status IN ('PENDING', 'APPROVED')
 GROUP BY requester_user_id, artist_id, album_id, track_number
 HAVING COUNT(*) > 1;
*/
--
-- OFFICIAL_RELEASE title 기준 중복 확인
/*
 SELECT
     requester_user_id,
     artist_id,
     album_id,
     title,
     COUNT(*) AS duplicate_count
 FROM track_applications
 WHERE track_type = 'OFFICIAL_RELEASE'
   AND status IN ('PENDING', 'APPROVED')
 GROUP BY requester_user_id, artist_id, album_id, title
 HAVING COUNT(*) > 1;
*/
-- Rollback 참고:
/*
 ALTER TABLE track_applications
     DROP INDEX uq_track_applications_free_creation_pending,
     DROP INDEX uq_track_applications_official_release_track_number,
     DROP INDEX uq_track_applications_official_release_title;

 ALTER TABLE track_applications
     DROP COLUMN free_creation_pending_key,
     DROP COLUMN official_release_track_number_key,
     DROP COLUMN official_release_title_key;

 ALTER TABLE tracks
     DROP INDEX uq_tracks_track_application_id,
     DROP COLUMN track_application_id;

 ALTER TABLE albums
     DROP INDEX uq_albums_album_application_id,
     DROP COLUMN album_application_id;

 ALTER TABLE artists
     DROP INDEX uq_artists_artist_application_id,
     DROP COLUMN artist_application_id;
*/

ALTER TABLE tracks
    ADD COLUMN track_application_id BIGINT NULL;

ALTER TABLE albums
    ADD COLUMN album_application_id BIGINT NULL;

ALTER TABLE artists
    ADD COLUMN artist_application_id BIGINT NULL;

ALTER TABLE tracks
    ADD UNIQUE INDEX uq_tracks_track_application_id (track_application_id),
    ALGORITHM=INPLACE,
    LOCK=NONE;

ALTER TABLE albums
    ADD UNIQUE INDEX uq_albums_album_application_id (album_application_id),
    ALGORITHM=INPLACE,
    LOCK=NONE;

ALTER TABLE artists
    ADD UNIQUE INDEX uq_artists_artist_application_id (artist_application_id),
    ALGORITHM=INPLACE,
    LOCK=NONE;

ALTER TABLE track_applications
    ADD COLUMN free_creation_pending_key CHAR(64)
        GENERATED ALWAYS AS (
            CASE
                WHEN track_type = 'FREE_CREATION'
                    AND status = 'PENDING'
                    THEN SHA2(
                        CONCAT_WS(
                                CHAR(31),
                                requester_user_id,
                                CHAR_LENGTH(COALESCE(title, '')),
                                COALESCE(title, ''),
                                CHAR_LENGTH(COALESCE(audio_url, '')),
                                COALESCE(audio_url, '')
                        ),
                        256
                         )
                ELSE NULL
                END
            ) STORED,

    ADD COLUMN official_release_track_number_key VARCHAR(500)
        GENERATED ALWAYS AS (
            CASE
                WHEN track_type = 'OFFICIAL_RELEASE'
                    AND status IN ('PENDING', 'APPROVED')
                    THEN CONCAT(
                        requester_user_id,
                        ':',
                        COALESCE(artist_id, 'NULL'),
                        ':',
                        COALESCE(album_id, 'NULL'),
                        ':',
                        COALESCE(track_number, 'NULL')
                         )
                ELSE NULL
                END
            ) STORED,

    ADD COLUMN official_release_title_key VARCHAR(500)
        GENERATED ALWAYS AS (
            CASE
                WHEN track_type = 'OFFICIAL_RELEASE'
                    AND status IN ('PENDING', 'APPROVED')
                    THEN CONCAT(
                        requester_user_id,
                        ':',
                        COALESCE(artist_id, 'NULL'),
                        ':',
                        COALESCE(album_id, 'NULL'),
                        ':',
                        title
                         )
                ELSE NULL
                END
            ) STORED;

ALTER TABLE track_applications
    ADD UNIQUE INDEX uq_track_applications_free_creation_pending
        (free_creation_pending_key),

    ADD UNIQUE INDEX uq_track_applications_official_release_track_number
        (official_release_track_number_key),

    ADD UNIQUE INDEX uq_track_applications_official_release_title
        (official_release_title_key),
    ALGORITHM=INPLACE,
    LOCK=NONE;
