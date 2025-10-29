ALTER TABLE evidence
    ADD redacted_size BIGINT;

ALTER TABLE custody_event
    ADD event_type VARCHAR(255) NOT NULL;

ALTER TABLE coc_report
    ALTER COLUMN sha256 TYPE VARCHAR(255) USING (sha256::VARCHAR(255));

ALTER TABLE evidence
    DROP COLUMN status;

ALTER TABLE evidence
    ADD status VARCHAR(255) NOT NULL;

ALTER TABLE processing_job
    ADD type VARCHAR(255) NOT NULL;