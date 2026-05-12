-- 트랙 오디오 저장값을 URL이 아닌 객체 스토리지 key로 관리한다.
-- V8에서 생성한 중복 방지 컬럼이 audio_url을 참조할 수 있으므로 물리 컬럼명 변경에 맞춰 재생성한다.
-- uq_track_applications_free_creation_pending이 존재하는 경우 제거 및 재생성 시
-- track_applications 테이블 메타데이터 락이 발생할 수 있다.
/*
rollback:

ALTER TABLE track_applications
    DROP INDEX uq_track_applications_free_creation_pending;

ALTER TABLE track_applications
    DROP COLUMN free_creation_pending_key;

ALTER TABLE track_applications
    RENAME COLUMN audio_key TO audio_url;

ALTER TABLE tracks
    RENAME COLUMN audio_key TO audio_url;

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
            ) STORED;

ALTER TABLE track_applications
    ADD UNIQUE INDEX uq_track_applications_free_creation_pending
        (free_creation_pending_key),
    ALGORITHM=INPLACE,
    LOCK=NONE;
*/

SET @drop_free_creation_pending_index_sql = IF(
        (
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'track_applications'
              AND INDEX_NAME = 'uq_track_applications_free_creation_pending'
        ) > 0,
        'ALTER TABLE track_applications DROP INDEX uq_track_applications_free_creation_pending',
        'SELECT 1'
                                         );
PREPARE drop_free_creation_pending_index_stmt FROM @drop_free_creation_pending_index_sql;
EXECUTE drop_free_creation_pending_index_stmt;
DEALLOCATE PREPARE drop_free_creation_pending_index_stmt;

SET @drop_free_creation_pending_column_sql = IF(
        (
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'track_applications'
              AND COLUMN_NAME = 'free_creation_pending_key'
        ) > 0,
        'ALTER TABLE track_applications DROP COLUMN free_creation_pending_key',
        'SELECT 1'
                                          );
PREPARE drop_free_creation_pending_column_stmt FROM @drop_free_creation_pending_column_sql;
EXECUTE drop_free_creation_pending_column_stmt;
DEALLOCATE PREPARE drop_free_creation_pending_column_stmt;

ALTER TABLE track_applications
    RENAME COLUMN audio_url TO audio_key;

ALTER TABLE tracks
    RENAME COLUMN audio_url TO audio_key;

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
                                COALESCE(title, '')
                        ),
                        256
                         )
                ELSE NULL
                END
            ) STORED;

ALTER TABLE track_applications
    ADD UNIQUE INDEX uq_track_applications_free_creation_pending
        (free_creation_pending_key),
    ALGORITHM=INPLACE,
    LOCK=NONE;
