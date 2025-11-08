-- Evidence rich metadata (images + video + capture context)

ALTER TABLE evidence
    ADD COLUMN IF NOT EXISTS exif_date_original      TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS tz_offset_minutes       INTEGER     NULL,
    ADD COLUMN IF NOT EXISTS capture_altitude_m      DOUBLE PRECISION NULL,
    ADD COLUMN IF NOT EXISTS capture_heading_deg     DOUBLE PRECISION NULL,

    ADD COLUMN IF NOT EXISTS camera_make             TEXT        NULL,
    ADD COLUMN IF NOT EXISTS camera_model            TEXT        NULL,
    ADD COLUMN IF NOT EXISTS lens_model              TEXT        NULL,
    ADD COLUMN IF NOT EXISTS software                TEXT        NULL,
    ADD COLUMN IF NOT EXISTS width_px                INTEGER     NULL,
    ADD COLUMN IF NOT EXISTS height_px               INTEGER     NULL,
    ADD COLUMN IF NOT EXISTS orientation_deg         INTEGER     NULL,

    ADD COLUMN IF NOT EXISTS container               TEXT        NULL,
    ADD COLUMN IF NOT EXISTS video_codec             TEXT        NULL,
    ADD COLUMN IF NOT EXISTS audio_codec             TEXT        NULL,
    ADD COLUMN IF NOT EXISTS duration_ms             BIGINT      NULL,
    ADD COLUMN IF NOT EXISTS video_fps               DOUBLE PRECISION NULL,
    ADD COLUMN IF NOT EXISTS video_rotation_deg      INTEGER     NULL;
