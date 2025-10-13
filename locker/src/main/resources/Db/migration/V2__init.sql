

CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public;



CREATE TYPE public.custody_event_type AS ENUM (
    'RECEIVED',
    'HASHED',
    'STORED_ORIGINAL',
    'QUEUED',
    'TRANSCODED',
    'REDACTED',
    'STORED_DERIVATIVE',
    'PDF'
);



CREATE TYPE public.evidence_status AS ENUM (
    'RECEIVED',
    'PROCESSING',
    'READY',
    'ERROR',
    'REDACTED'
);




CREATE TYPE public.job_status AS ENUM (
    'QUEUED',
    'RUNNING',
    'SUCCESS',
    'FAILED'
);




CREATE TYPE public.job_type AS ENUM (
    'TRANSCODE',
    'REDACT',
    'THUMBNAIL',
    'GENERATE_COC'
);




CREATE TABLE public.app_user (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    email text NOT NULL,
    password_hash text NOT NULL,
    display_name text,
    role text DEFAULT 'USER'::text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);




CREATE TABLE public.coc_report (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    evidence_id uuid NOT NULL,
    pdf_key text NOT NULL,
    sha256 text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);




CREATE TABLE public.custody_event (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    evidence_id uuid NOT NULL,
    actor_user_id uuid,
    event_type public.custody_event_type NOT NULL,
    meta_json jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);




CREATE TABLE public.evidence (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    owner_user_id uuid NOT NULL,
    title text,
    description text,
    captured_at timestamp with time zone,
    capture_latlon public.geography(Point,4326),
    capture_accuracy_m numeric,
    status public.evidence_status DEFAULT 'RECEIVED'::public.evidence_status NOT NULL,
    original_sha256 text NOT NULL,
    original_size_b bigint NOT NULL,
    original_key text NOT NULL,
    derivative_key text,
    thumbnail_key text,
    legal_hold boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);



CREATE TABLE public.processing_job (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    evidence_id uuid NOT NULL,
    type public.job_type NOT NULL,
    status public.job_status DEFAULT 'QUEUED'::public.job_status NOT NULL,
    attempts integer DEFAULT 0 NOT NULL,
    error_msg text,
    payload_json jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);




CREATE TABLE public.share_link (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    evidence_id uuid NOT NULL,
    created_by uuid NOT NULL,
    token text NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    allow_original boolean DEFAULT false NOT NULL,
    revoked_at timestamp with time zone
);




CREATE TABLE public.user_session (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    jwt_id text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    expires_at timestamp with time zone NOT NULL
);




CREATE TABLE public.webhook_event (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    provider text NOT NULL,
    event_type text NOT NULL,
    external_id text NOT NULL,
    payload_json jsonb NOT NULL,
    processed boolean DEFAULT false NOT NULL,
    processed_at timestamp with time zone
);




ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT app_user_email_key UNIQUE (email);



ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT app_user_pkey PRIMARY KEY (id);




ALTER TABLE ONLY public.coc_report
    ADD CONSTRAINT coc_report_pkey PRIMARY KEY (id);




ALTER TABLE ONLY public.custody_event
    ADD CONSTRAINT custody_event_pkey PRIMARY KEY (id);




ALTER TABLE ONLY public.evidence
    ADD CONSTRAINT evidence_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.processing_job
    ADD CONSTRAINT processing_job_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.share_link
    ADD CONSTRAINT share_link_pkey PRIMARY KEY (id);




ALTER TABLE ONLY public.share_link
    ADD CONSTRAINT share_link_token_key UNIQUE (token);



ALTER TABLE ONLY public.user_session
    ADD CONSTRAINT user_session_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.webhook_event
    ADD CONSTRAINT webhook_event_pkey PRIMARY KEY (id);




CREATE INDEX custody_event_evidence_id_idx ON public.custody_event USING btree (evidence_id);



CREATE INDEX evidence_captured_at_idx ON public.evidence USING btree (captured_at);




CREATE INDEX evidence_owner_user_id_idx ON public.evidence USING btree (owner_user_id);




CREATE INDEX evidence_status_idx ON public.evidence USING btree (status);




CREATE INDEX share_link_token_idx ON public.share_link USING btree (token);




ALTER TABLE ONLY public.coc_report
    ADD CONSTRAINT coc_report_evidence_id_fkey FOREIGN KEY (evidence_id) REFERENCES public.evidence(id) ON DELETE CASCADE;




ALTER TABLE ONLY public.custody_event
    ADD CONSTRAINT custody_event_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES public.app_user(id);




ALTER TABLE ONLY public.custody_event
    ADD CONSTRAINT custody_event_evidence_id_fkey FOREIGN KEY (evidence_id) REFERENCES public.evidence(id) ON DELETE CASCADE;



ALTER TABLE ONLY public.evidence
    ADD CONSTRAINT evidence_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.app_user(id) ON DELETE SET NULL;




ALTER TABLE ONLY public.processing_job
    ADD CONSTRAINT processing_job_evidence_id_fkey FOREIGN KEY (evidence_id) REFERENCES public.evidence(id) ON DELETE CASCADE;




ALTER TABLE ONLY public.share_link
    ADD CONSTRAINT share_link_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.app_user(id) ON DELETE CASCADE;


ALTER TABLE ONLY public.share_link
    ADD CONSTRAINT share_link_evidence_id_fkey FOREIGN KEY (evidence_id) REFERENCES public.evidence(id) ON DELETE CASCADE;




ALTER TABLE ONLY public.user_session
    ADD CONSTRAINT user_session_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.app_user(id) ON DELETE CASCADE;


