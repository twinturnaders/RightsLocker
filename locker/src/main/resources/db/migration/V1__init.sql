CREATE EXTENSION IF NOT EXISTS Postgis;

CREATE TYPE public.custody_event_type AS ENUM (
    'RECEIVED', 'HASHED', 'STORED_ORIGINAL', 'QUEUED', 'TRANSCODED', 'REDACTED',
    'STORED_DERIVATIVE',
    'PDF_GENERATED',
    'DOWNLOADED',
    'SHARED',
    'LEGAL_HOLD_ON',
    'LEGAL_HOLD_OFF',
    'THUMBNAIL_GENERATED',
    'REVOKED'

    );


--
-- Name: evidence_status; Type: TYPE; Schema: public;-
--

CREATE TYPE public.evidence_status AS ENUM (
    'RECEIVED',
    'PROCESSING',
    'READY',
    'ERROR',
    'REDACTED'
    );


--
-- Name: job_status; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.job_status AS ENUM (
    'QUEUED',
    'RUNNING',
    'SUCCESS',
    'SUCCEEDED',
    'ERROR',
    'DONE',
    'FAILED'
    );


--
-- Name: job_type; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.job_type AS ENUM (
    'TRANSCODE',
    'REDACT',
    'THUMBNAIL',
    'GENERATE_COC'
    );


--
-- Name: role; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.role AS ENUM (
    'USER',
    'MOD',
    'ADMIN'
    );


--
-- Name: app_user; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.app_user (
                                 id uuid DEFAULT gen_random_uuid() NOT NULL,
                                 email text NOT NULL,
                                 password_hash text NOT NULL,
                                 display_name text,
                                 role text DEFAULT 'USER'::text NOT NULL,
                                 created_at timestamp with time zone DEFAULT now() NOT NULL,
                                 updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: coc_report; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.coc_report (
                                   id uuid DEFAULT gen_random_uuid() NOT NULL,
                                   evidence_id uuid NOT NULL,
                                   pdf_key text NOT NULL,
                                   sha256 text NOT NULL,
                                   created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: custody_event; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.custody_event (
                                      id uuid DEFAULT gen_random_uuid() NOT NULL,
                                      evidence_id uuid NOT NULL,
                                      actor_user_id uuid,
                                      event_type public.custody_event_type NOT NULL,
                                      meta_json jsonb,
                                      created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: evidence; Type: TABLE; Schema: public; Owner: -
--

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


--
-- Name: processing_job; Type: TABLE; Schema: public; Owner: -
--

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


--
-- Name: share_link; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.share_link (
                                   id uuid DEFAULT gen_random_uuid() NOT NULL,
                                   evidence_id uuid NOT NULL,
                                   created_by uuid NOT NULL,
                                   token text NOT NULL,
                                   expires_at timestamp with time zone NOT NULL,
                                   allow_original boolean DEFAULT false NOT NULL,
                                   revoked_at timestamp with time zone
);


--
-- Name: user_session; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_session (
                                     id uuid DEFAULT gen_random_uuid() NOT NULL,
                                     user_id uuid NOT NULL,
                                     jwt_id text NOT NULL,
                                     created_at timestamp with time zone DEFAULT now() NOT NULL,
                                     expires_at timestamp with time zone NOT NULL
);


--
-- Name: webhook_event; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.webhook_event (
                                      id uuid DEFAULT gen_random_uuid() NOT NULL,
                                      provider text NOT NULL,
                                      event_type text NOT NULL,
                                      external_id text NOT NULL,
                                      payload_json jsonb NOT NULL,
                                      processed boolean DEFAULT false NOT NULL,
                                      processed_at timestamp with time zone
);


--
-- Name: app_user app_user_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT app_user_email_key UNIQUE (email);


--
-- Name: app_user app_user_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT app_user_pkey PRIMARY KEY (id);


--
-- Name: coc_report coc_report_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.coc_report
    ADD CONSTRAINT coc_report_pkey PRIMARY KEY (id);


--
-- Name: custody_event custody_event_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.custody_event
    ADD CONSTRAINT custody_event_pkey PRIMARY KEY (id);


--
-- Name: evidence evidence_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evidence
    ADD CONSTRAINT evidence_pkey PRIMARY KEY (id);


--
-- Name: processing_job processing_job_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.processing_job
    ADD CONSTRAINT processing_job_pkey PRIMARY KEY (id);


--
-- Name: share_link share_link_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.share_link
    ADD CONSTRAINT share_link_pkey PRIMARY KEY (id);


--
-- Name: share_link share_link_token_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.share_link
    ADD CONSTRAINT share_link_token_key UNIQUE (token);


--
-- Name: user_session user_session_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_session
    ADD CONSTRAINT user_session_pkey PRIMARY KEY (id);


--
-- Name: webhook_event webhook_event_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.webhook_event
    ADD CONSTRAINT webhook_event_pkey PRIMARY KEY (id);


--
-- Name: custody_event_evidence_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX custody_event_evidence_id_idx ON public.custody_event USING btree (evidence_id);


--
-- Name: evidence_captured_at_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX evidence_captured_at_idx ON public.evidence USING btree (captured_at);


--
-- Name: evidence_owner_user_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX evidence_owner_user_id_idx ON public.evidence USING btree (owner_user_id);


--
-- Name: evidence_status_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX evidence_status_idx ON public.evidence USING btree (status);


--
-- Name: share_link_token_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX share_link_token_idx ON public.share_link USING btree (token);


--
-- Name: coc_report coc_report_evidence_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.coc_report
    ADD CONSTRAINT coc_report_evidence_id_fkey FOREIGN KEY (evidence_id) REFERENCES public.evidence(id) ON DELETE CASCADE;


--
-- Name: custody_event custody_event_actor_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.custody_event
    ADD CONSTRAINT custody_event_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES public.app_user(id);


--
-- Name: custody_event custody_event_evidence_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.custody_event
    ADD CONSTRAINT custody_event_evidence_id_fkey FOREIGN KEY (evidence_id) REFERENCES public.evidence(id) ON DELETE CASCADE;


--
-- Name: evidence evidence_owner_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evidence
    ADD CONSTRAINT evidence_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.app_user(id) ON DELETE SET NULL;


--
-- Name: processing_job processing_job_evidence_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.processing_job
    ADD CONSTRAINT processing_job_evidence_id_fkey FOREIGN KEY (evidence_id) REFERENCES public.evidence(id) ON DELETE CASCADE;


--
-- Name: share_link share_link_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.share_link
    ADD CONSTRAINT share_link_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.app_user(id) ON DELETE CASCADE;


--
-- Name: share_link share_link_evidence_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.share_link
    ADD CONSTRAINT share_link_evidence_id_fkey FOREIGN KEY (evidence_id) REFERENCES public.evidence(id) ON DELETE CASCADE;


--
-- Name: user_session user_session_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_session
    ADD CONSTRAINT user_session_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.app_user(id) ON DELETE CASCADE;
ALTER TABLE Evidence ADD COLUMN IF NOT EXISTS thumbnail_key text;
ALTER TABLE Evidence ADD COLUMN IF NOT EXISTS redacted_key  text;

