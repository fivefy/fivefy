ALTER TABLE tracks
    ADD COLUMN track_application_id BIGINT NULL,
    ADD UNIQUE INDEX uq_tracks_track_application_id (track_application_id);

ALTER TABLE albums
    ADD COLUMN album_application_id BIGINT NULL,
    ADD UNIQUE INDEX uq_albums_album_application_id (album_application_id);

ALTER TABLE artists
    ADD COLUMN artist_application_id BIGINT NULL,
    ADD UNIQUE INDEX uq_artists_artist_application_id (artist_application_id);

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
                                CHAR_LENGTH(title),
                                title,
                                CHAR_LENGTH(audio_url),
                                audio_url
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
                        artist_id,
                        ':',
                        album_id,
                        ':',
                        track_number
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
                        artist_id,
                        ':',
                        album_id,
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
        (official_release_title_key);
