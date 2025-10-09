CREATE EXTENSION IF NOT EXISTS postgis;
CREATE TABLE app_user (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
email TEXT UNIQUE NOT NULL,
password_hash TEXT NOT NULL,
display_name TEXT,
role TEXT NOT NULL DEFAULT 'USER', -- USER, MOD, ADMIN
created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE user_session (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
jwt_id TEXT NOT NULL,
created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
expires_at TIMESTAMPTZ NOT NULL
);
-- Evidence core
CREATE TYPE evidence_status AS ENUM
('RECEIVED','PROCESSING','READY','ERROR','REDACTED');
CREATE TABLE evidence (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
owner_user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE SET NULL,
title TEXT,
description TEXT,
captured_at TIMESTAMPTZ,
capture_latlon GEOGRAPHY(POINT, 4326),
capture_accuracy_m NUMERIC,
status evidence_status NOT NULL DEFAULT 'RECEIVED',
original_sha256 TEXT NOT NULL,
original_size_b BIGINT NOT NULL,
original_key TEXT NOT NULL, -- s3 key in originals
derivative_key TEXT, -- s3 key in hot (redacted
thumbnail_key TEXT,
legal_hold BOOLEAN NOT NULL DEFAULT FALSE,
created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- Processing jobs
CREATE TYPE job_type AS ENUM ('TRANSCODE','REDACT','THUMBNAIL','GENERATE_COC');
CREATE TYPE job_status AS ENUM ('QUEUED','RUNNING','SUCCESS','FAILED');
CREATE TABLE processing_job (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
evidence_id UUID NOT NULL REFERENCES evidence(id) ON DELETE CASCADE,
type job_type NOT NULL,
status job_status NOT NULL DEFAULT 'QUEUED',
attempts INT NOT NULL DEFAULT 0,
error_msg TEXT,
payload_json JSONB,
created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- Custody events (auditable trail)
CREATE TYPE custody_event_type AS ENUM
('RECEIVED','HASHED','STORED_ORIGINAL','QUEUED','TRANSCODED','REDACTED','STORED_DERIVATIVE','PDF');
CREATE TABLE custody_event (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
evidence_id UUID NOT NULL REFERENCES evidence(id) ON DELETE CASCADE,
actor_user_id UUID REFERENCES app_user(id),
event_type custody_event_type NOT NULL,
meta_json JSONB,
created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- Share links (expiring access)
CREATE TABLE share_link (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
evidence_id UUID NOT NULL REFERENCES evidence(id) ON DELETE CASCADE,
created_by UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
token TEXT NOT NULL UNIQUE,
expires_at TIMESTAMPTZ NOT NULL,
allow_original BOOLEAN NOT NULL DEFAULT FALSE,
revoked_at TIMESTAMPTZ
);
-- Chain-of-custody PDFs
CREATE TABLE coc_report (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
evidence_id UUID NOT NULL REFERENCES evidence(id) ON DELETE CASCADE,
pdf_key TEXT NOT NULL, -- s3 key in hot
sha256 TEXT NOT NULL,
created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- Webhook audit (for future external integrations)
CREATE TABLE webhook_event (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
provider TEXT NOT NULL,
event_type TEXT NOT NULL,
external_id TEXT NOT NULL,
payload_json JSONB NOT NULL,
processed BOOLEAN NOT NULL DEFAULT FALSE,
processed_at TIMESTAMPTZ
);
CREATE INDEX ON evidence(owner_user_id);
CREATE INDEX ON evidence(status);
CREATE INDEX ON evidence(captured_at);
CREATE INDEX ON custody_event(evidence_id);
CREATE INDEX ON share_link(token);
