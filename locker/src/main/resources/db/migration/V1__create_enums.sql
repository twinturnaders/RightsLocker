DO $$
    BEGIN

        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'custody_event_type') THEN
            CREATE TYPE custody_event_type AS ENUM ('BOOKED','TRANSFERRED','RELEASED'); -- adjust values
        END IF;
    END $$;

DO $$
    BEGIN

        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'evidence_status') THEN
            CREATE TYPE evidence_status AS ENUM ('RECEIVED', 'PROCESSING', 'READY', 'ERROR', 'REDACTED'); -- adjust values
        END IF;
    END $$;

DO $$
    BEGIN

        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'job_status') THEN
            CREATE TYPE job_status AS ENUM ('QUEUED', 'RUNNING', 'SUCCESS', 'SUCCEEDED', 'ERROR', 'FAILED'); -- adjust values
        END IF;
    END $$;

DO $$
    BEGIN

        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'role') THEN
            CREATE TYPE role AS ENUM ('USER', 'MOD', 'ADMIN'); -- adjust values
        END IF;
    END $$;

DO $$
    BEGIN

        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'job_type') THEN
            CREATE TYPE job_type AS ENUM ('TRANSCODE', 'REDACT', 'THUMBNAIL', 'GENERATE_COC'); -- adjust values
        END IF;
    END $$;