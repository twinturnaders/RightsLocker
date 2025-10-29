

CREATE SCHEMA public;




CREATE TYPE public.custody_event_type AS ENUM (
    'BOOKED',
    'TRANSFERRED',
    'RELEASED',
    'RECEIVED',
    'HASHED',
    'STORED_ORIGINAL',
    'QUEUED',
    'TRANSCODED',
    'REDACTED',
    'STORED_DERIVATIVE',
    'PDF_GENERATED',
    'DOWNLOADED',
    'SHARED',
    'LEGAL_HOLD_ON'
);


--
-- TOC entry 1616 (class 1247 OID 37640)
-- Name: evidence_status; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.evidence_status AS ENUM (
    'RECEIVED',
    'PROCESSING',
    'READY',
    'ERROR',
    'REDACTED'
);


--
-- TOC entry 1619 (class 1247 OID 37652)
-- Name: job_status; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.job_status AS ENUM (
    'QUEUED',
    'RUNNING',
    'SUCCESS',
    'SUCCEEDED',
    'ERROR',
    'FAILED',
    'DONE'
);


--
-- TOC entry 1625 (class 1247 OID 37674)
-- Name: job_type; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.job_type AS ENUM (
    'TRANSCODE',
    'REDACT',
    'THUMBNAIL',
    'GENERATE_COC'
);


--
-- TOC entry 1622 (class 1247 OID 37666)
-- Name: role; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.role AS ENUM (
    'USER',
    'MOD',
    'ADMIN'
);


--
-- TOC entry 223 (class 1259 OID 38763)
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
-- TOC entry 229 (class 1259 OID 38864)
-- Name: coc_report; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.coc_report (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    evidence_id uuid NOT NULL,
    pdf_key text NOT NULL,
    sha256 character varying(255) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- TOC entry 227 (class 1259 OID 38824)
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
-- TOC entry 225 (class 1259 OID 38790)
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
    original_sha256 text NOT NULL,
    original_size_b bigint NOT NULL,
    original_key text NOT NULL,
    derivative_key text,
    thumbnail_key text,
    legal_hold boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    redacted_key text,
    redacted_size bigint,
    status character varying(255) NOT NULL
);


--
-- TOC entry 226 (class 1259 OID 38807)
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
-- TOC entry 228 (class 1259 OID 38843)
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
-- TOC entry 224 (class 1259 OID 38776)
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
-- TOC entry 230 (class 1259 OID 38878)
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
-- TOC entry 5723 (class 2606 OID 38775)
-- Name: app_user app_user_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT app_user_email_key UNIQUE (email);


--
-- TOC entry 5725 (class 2606 OID 38773)
-- Name: app_user app_user_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT app_user_pkey PRIMARY KEY (id);


--
-- TOC entry 5743 (class 2606 OID 38872)
-- Name: coc_report coc_report_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.coc_report
    ADD CONSTRAINT coc_report_pkey PRIMARY KEY (id);


--
-- TOC entry 5736 (class 2606 OID 38832)
-- Name: custody_event custody_event_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.custody_event
    ADD CONSTRAINT custody_event_pkey PRIMARY KEY (id);


--
-- TOC entry 5731 (class 2606 OID 38801)
-- Name: evidence evidence_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evidence
    ADD CONSTRAINT evidence_pkey PRIMARY KEY (id);


--
-- TOC entry 5733 (class 2606 OID 38818)
-- Name: processing_job processing_job_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.processing_job
    ADD CONSTRAINT processing_job_pkey PRIMARY KEY (id);


--
-- TOC entry 5738 (class 2606 OID 38851)
-- Name: share_link share_link_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.share_link
    ADD CONSTRAINT share_link_pkey PRIMARY KEY (id);


--
-- TOC entry 5741 (class 2606 OID 38853)
-- Name: share_link share_link_token_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.share_link
    ADD CONSTRAINT share_link_token_key UNIQUE (token);


--
-- TOC entry 5727 (class 2606 OID 38784)
-- Name: user_session user_session_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_session
    ADD CONSTRAINT user_session_pkey PRIMARY KEY (id);


--
-- TOC entry 5745 (class 2606 OID 38886)
-- Name: webhook_event webhook_event_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.webhook_event
    ADD CONSTRAINT webhook_event_pkey PRIMARY KEY (id);


--
-- TOC entry 5734 (class 1259 OID 38890)
-- Name: custody_event_evidence_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX custody_event_evidence_id_idx ON public.custody_event USING btree (evidence_id);


--
-- TOC entry 5728 (class 1259 OID 38889)
-- Name: evidence_captured_at_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX evidence_captured_at_idx ON public.evidence USING btree (captured_at);


--
-- TOC entry 5729 (class 1259 OID 38887)
-- Name: evidence_owner_user_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX evidence_owner_user_id_idx ON public.evidence USING btree (owner_user_id);


--
-- TOC entry 5739 (class 1259 OID 38891)
-- Name: share_link_token_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX share_link_token_idx ON public.share_link USING btree (token);


--
-- TOC entry 5753 (class 2606 OID 38873)
-- Name: coc_report coc_report_evidence_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.coc_report
    ADD CONSTRAINT coc_report_evidence_id_fkey FOREIGN KEY (evidence_id) REFERENCES public.evidence(id) ON DELETE CASCADE;


--
-- TOC entry 5749 (class 2606 OID 38838)
-- Name: custody_event custody_event_actor_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.custody_event
    ADD CONSTRAINT custody_event_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES public.app_user(id);


--
-- TOC entry 5750 (class 2606 OID 38833)
-- Name: custody_event custody_event_evidence_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.custody_event
    ADD CONSTRAINT custody_event_evidence_id_fkey FOREIGN KEY (evidence_id) REFERENCES public.evidence(id) ON DELETE CASCADE;


--
-- TOC entry 5747 (class 2606 OID 38802)
-- Name: evidence evidence_owner_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evidence
    ADD CONSTRAINT evidence_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.app_user(id) ON DELETE SET NULL;


--
-- TOC entry 5748 (class 2606 OID 38819)
-- Name: processing_job processing_job_evidence_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.processing_job
    ADD CONSTRAINT processing_job_evidence_id_fkey FOREIGN KEY (evidence_id) REFERENCES public.evidence(id) ON DELETE CASCADE;


--
-- TOC entry 5751 (class 2606 OID 38859)
-- Name: share_link share_link_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.share_link
    ADD CONSTRAINT share_link_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.app_user(id) ON DELETE CASCADE;


--
-- TOC entry 5752 (class 2606 OID 38854)
-- Name: share_link share_link_evidence_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.share_link
    ADD CONSTRAINT share_link_evidence_id_fkey FOREIGN KEY (evidence_id) REFERENCES public.evidence(id) ON DELETE CASCADE;


--
-- TOC entry 5746 (class 2606 OID 38785)
-- Name: user_session user_session_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_session
    ADD CONSTRAINT user_session_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.app_user(id) ON DELETE CASCADE;



