

CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public;


--
-- TOC entry 1657 (class 1247 OID 24683)
-- Name: custody_event_type; Type: TYPE; Schema: public; Owner: -
--

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


--
-- TOC entry 1642 (class 1247 OID 24619)
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
-- TOC entry 1651 (class 1247 OID 24656)
-- Name: job_status; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.job_status AS ENUM (
    'QUEUED',
    'RUNNING',
    'SUCCESS',
    'FAILED'
);


--
-- TOC entry 1648 (class 1247 OID 24647)
-- Name: job_type; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.job_type AS ENUM (
    'TRANSCODE',
    'REDACT',
    'THUMBNAIL',
    'GENERATE_COC'
);


--
-- TOC entry 223 (class 1259 OID 24591)
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
-- TOC entry 229 (class 1259 OID 24739)
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
-- TOC entry 227 (class 1259 OID 24699)
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
-- TOC entry 225 (class 1259 OID 24629)
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
-- TOC entry 226 (class 1259 OID 24665)
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
-- TOC entry 228 (class 1259 OID 24718)
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
-- TOC entry 224 (class 1259 OID 24604)
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
-- TOC entry 230 (class 1259 OID 24753)
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
-- TOC entry 5721 (class 2606 OID 24603)
-- Name: app_user app_user_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT app_user_email_key UNIQUE (email);


--
-- TOC entry 5723 (class 2606 OID 24601)
-- Name: app_user app_user_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT app_user_pkey PRIMARY KEY (id);


--
-- TOC entry 5742 (class 2606 OID 24747)
-- Name: coc_report coc_report_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.coc_report
    ADD CONSTRAINT coc_report_pkey PRIMARY KEY (id);


--
-- TOC entry 5735 (class 2606 OID 24707)
-- Name: custody_event custody_event_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.custody_event
    ADD CONSTRAINT custody_event_pkey PRIMARY KEY (id);


--
-- TOC entry 5729 (class 2606 OID 24640)
-- Name: evidence evidence_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evidence
    ADD CONSTRAINT evidence_pkey PRIMARY KEY (id);


--
-- TOC entry 5732 (class 2606 OID 24676)
-- Name: processing_job processing_job_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.processing_job
    ADD CONSTRAINT processing_job_pkey PRIMARY KEY (id);


--
-- TOC entry 5737 (class 2606 OID 24726)
-- Name: share_link share_link_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.share_link
    ADD CONSTRAINT share_link_pkey PRIMARY KEY (id);


--
-- TOC entry 5740 (class 2606 OID 24728)
-- Name: share_link share_link_token_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.share_link
    ADD CONSTRAINT share_link_token_key UNIQUE (token);


--
-- TOC entry 5725 (class 2606 OID 24612)
-- Name: user_session user_session_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_session
    ADD CONSTRAINT user_session_pkey PRIMARY KEY (id);


--
-- TOC entry 5744 (class 2606 OID 24761)
-- Name: webhook_event webhook_event_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.webhook_event
    ADD CONSTRAINT webhook_event_pkey PRIMARY KEY (id);


--
-- TOC entry 5733 (class 1259 OID 24765)
-- Name: custody_event_evidence_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX custody_event_evidence_id_idx ON public.custody_event USING btree (evidence_id);


--
-- TOC entry 5726 (class 1259 OID 24764)
-- Name: evidence_captured_at_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX evidence_captured_at_idx ON public.evidence USING btree (captured_at);


--
-- TOC entry 5727 (class 1259 OID 24762)
-- Name: evidence_owner_user_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX evidence_owner_user_id_idx ON public.evidence USING btree (owner_user_id);


--
-- TOC entry 5730 (class 1259 OID 24763)
-- Name: evidence_status_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX evidence_status_idx ON public.evidence USING btree (status);


--
-- TOC entry 5738 (class 1259 OID 24766)
-- Name: share_link_token_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX share_link_token_idx ON public.share_link USING btree (token);


--
-- TOC entry 5752 (class 2606 OID 24748)
-- Name: coc_report coc_report_evidence_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.coc_report
    ADD CONSTRAINT coc_report_evidence_id_fkey FOREIGN KEY (evidence_id) REFERENCES public.evidence(id) ON DELETE CASCADE;


--
-- TOC entry 5748 (class 2606 OID 24713)
-- Name: custody_event custody_event_actor_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.custody_event
    ADD CONSTRAINT custody_event_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES public.app_user(id);


--
-- TOC entry 5749 (class 2606 OID 24708)
-- Name: custody_event custody_event_evidence_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.custody_event
    ADD CONSTRAINT custody_event_evidence_id_fkey FOREIGN KEY (evidence_id) REFERENCES public.evidence(id) ON DELETE CASCADE;


--
-- TOC entry 5746 (class 2606 OID 24641)
-- Name: evidence evidence_owner_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evidence
    ADD CONSTRAINT evidence_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.app_user(id) ON DELETE SET NULL;


--
-- TOC entry 5747 (class 2606 OID 24677)
-- Name: processing_job processing_job_evidence_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.processing_job
    ADD CONSTRAINT processing_job_evidence_id_fkey FOREIGN KEY (evidence_id) REFERENCES public.evidence(id) ON DELETE CASCADE;


--
-- TOC entry 5750 (class 2606 OID 24734)
-- Name: share_link share_link_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.share_link
    ADD CONSTRAINT share_link_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.app_user(id) ON DELETE CASCADE;


--
-- TOC entry 5751 (class 2606 OID 24729)
-- Name: share_link share_link_evidence_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.share_link
    ADD CONSTRAINT share_link_evidence_id_fkey FOREIGN KEY (evidence_id) REFERENCES public.evidence(id) ON DELETE CASCADE;


--
-- TOC entry 5745 (class 2606 OID 24613)
-- Name: user_session user_session_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_session
    ADD CONSTRAINT user_session_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.app_user(id) ON DELETE CASCADE;


-- Completed on 2025-10-09 13:17:48

--
-- PostgreSQL database dump complete
--

\unrestrict aaiLbzQmfhRIjAeCMAqozaR1RcpUQrGa8Z9IINgwcnxhcO2HNEVhHwF78p2uoCc

