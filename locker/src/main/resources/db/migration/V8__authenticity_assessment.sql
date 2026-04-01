ALTER TABLE evidence
    ADD COLUMN IF NOT EXISTS provenance_status TEXT NULL,
    ADD COLUMN IF NOT EXISTS metadata_integrity TEXT NULL,
    ADD COLUMN IF NOT EXISTS synthetic_media_risk TEXT NULL,
    ADD COLUMN IF NOT EXISTS manipulation_signals TEXT NULL,
    ADD COLUMN IF NOT EXISTS assessment_summary TEXT NULL;
