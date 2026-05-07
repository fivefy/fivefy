ALTER TABLE album_applications
    ADD COLUMN active_duplicate_key VARCHAR(500)
        GENERATED ALWAYS AS (
            CASE
                WHEN status IN ('PENDING', 'APPROVED')
                    THEN CONCAT(requester_user_id, ':', artist_id, ':', title)
                ELSE NULL
                END
            ) STORED;

CREATE UNIQUE INDEX uq_album_application_active_duplicate_key
    ON album_applications (active_duplicate_key);

ALTER TABLE artist_applications
    ADD COLUMN active_duplicate_key VARCHAR(500)
        GENERATED ALWAYS AS (
            CASE
                WHEN status IN ('PENDING', 'APPROVED')
                    THEN CONCAT(requester_user_id, ':', requested_name, ':', artist_type)
                ELSE NULL
                END
            ) STORED;

CREATE UNIQUE INDEX uq_artist_application_active_duplicate_key
    ON artist_applications (active_duplicate_key);