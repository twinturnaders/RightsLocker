ALTER TABLE custody_event
    ALTER COLUMN event_type TYPE varchar USING event_type::text;


ALTER TABLE processing_job
    ALTER COLUMN type TYPE varchar USING job_type::text;

ALTER TABLE processing_job
    ALTER COLUMN status TYPE varchar USING job_status::text;

ALTER TABLE evidence
    ALTER COLUMN status TYPE varchar USING evidence_status::text;

ALTER TABLE app_user
    ALTER COLUMN role TYPE varchar USING role::text;
