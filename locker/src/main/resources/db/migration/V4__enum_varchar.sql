ALTER TABLE custody_event
    ALTER COLUMN event_type TYPE text USING event_type::text;


ALTER TABLE processing_job
    ALTER COLUMN type TYPE text USING type::text;

ALTER TABLE processing_job
    ALTER COLUMN status TYPE text USING status::text;

ALTER TABLE evidence
    ALTER COLUMN status TYPE text USING status::text;

ALTER TABLE app_user
    ALTER COLUMN role TYPE text USING role::text;
