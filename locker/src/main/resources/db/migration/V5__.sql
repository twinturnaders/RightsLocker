ALTER TABLE share_link
    DROP CONSTRAINT share_link_created_by_fkey;

ALTER TABLE share_link
    DROP CONSTRAINT share_link_evidence_id_fkey;

ALTER TABLE share_link
    ADD created_at TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE share_link
    ALTER COLUMN created_at SET NOT NULL;

CREATE INDEX ix_share_evidence_id ON share_link (evidence_id);

ALTER TABLE share_link
    DROP COLUMN created_by;