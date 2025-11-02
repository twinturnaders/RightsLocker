ALTER TABLE evidence
    ALTER COLUMN owner_user_id DROP NOT NULL;

ALTER TABLE evidence
    DROP CONSTRAINT IF EXISTS fk_evidence_owner;

ALTER TABLE evidence
    ADD CONSTRAINT fk_evidence_owner
        FOREIGN KEY (owner_user_id) REFERENCES app_user(id)
            ON DELETE SET NULL;