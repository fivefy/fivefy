-- Track audio storage is now modeled as an object storage key, not a persisted URL.
-- V8's generated duplicate key references audio_url, so recreate that generated
-- column around the physical column rename.

ALTER TABLE track_applications
    DROP INDEX uq_track_applications_free_creation_pending;

ALTER TABLE track_applications
    DROP COLUMN free_creation_pending_key;

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
