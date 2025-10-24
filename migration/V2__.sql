ALTER TABLE evidence
    ADD redacted_size BIGINT;

ALTER TABLE custody_event
    ADD event_type VARCHAR(255) NOT NULL;

ALTER TABLE evidence
    ADD status VARCHAR(255) NOT NULL;