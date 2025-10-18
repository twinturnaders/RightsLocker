CREATE TABLE app_user
(
    id            CHAR(36)                    DEFAULT gen_random_uuid() NOT NULL,
    email         TEXT                                                  NOT NULL,
    password_hash TEXT                                                  NOT NULL,
    display_name  TEXT,
    role          TEXT                        DEFAULT 'USER'            NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    updated_at    TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    CONSTRAINT app_user_pkey PRIMARY KEY (id)
);

CREATE TABLE coc_report
(
    id          CHAR(36)                    DEFAULT gen_random_uuid() NOT NULL,
    evidence_id CHAR(36)                                              NOT NULL,
    pdf_key     TEXT                                                  NOT NULL,
    sha256      TEXT                                                  NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    CONSTRAINT coc_report_pkey PRIMARY KEY (id)
);

CREATE TABLE custody_event
(
    id            CHAR(36)                    DEFAULT gen_random_uuid() NOT NULL,
    evidence_id   CHAR(36)                                              NOT NULL,
    actor_user_id CHAR(36),
    event_type    CUSTODY_EVENT_TYPE                                    NOT NULL,
    meta_json     JSONB,
    created_at    TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    CONSTRAINT custody_event_pkey PRIMARY KEY (id)
);

CREATE TABLE evidence
(
    id                 CHAR(36)                    DEFAULT gen_random_uuid() NOT NULL,
    owner_user_id      CHAR(36)                                              NOT NULL,
    title              TEXT,
    description        TEXT,
    captured_at        TIMESTAMP WITHOUT TIME ZONE,
    capture_latlon     GEOGRAPHY,
    capture_accuracy_m numeric,
    status             EVIDENCE_STATUS             DEFAULT 'RECEIVED'        NOT NULL,
    original_sha256    TEXT                                                  NOT NULL,
    original_size_b    BIGINT                                                NOT NULL,
    original_key       TEXT                                                  NOT NULL,
    derivative_key     TEXT,
    thumbnail_key      TEXT,
    legal_hold         BOOLEAN                     DEFAULT FALSE             NOT NULL,
    created_at         TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    updated_at         TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    CONSTRAINT evidence_pkey PRIMARY KEY (id)
);

CREATE TABLE processing_job
(
    id           CHAR(36)                    DEFAULT gen_random_uuid() NOT NULL,
    evidence_id  CHAR(36)                                              NOT NULL,
    type         JOB_TYPE                                              NOT NULL,
    status       JOB_STATUS                  DEFAULT 'QUEUED'          NOT NULL,
    attempts     INTEGER                     DEFAULT 0                 NOT NULL,
    error_msg    TEXT,
    payload_json JSONB,
    created_at   TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    updated_at   TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    CONSTRAINT processing_job_pkey PRIMARY KEY (id)
);

CREATE TABLE share_link
(
    id             CHAR(36) DEFAULT gen_random_uuid() NOT NULL,
    evidence_id    CHAR(36)                           NOT NULL,
    created_by     CHAR(36)                           NOT NULL,
    token          TEXT                               NOT NULL,
    expires_at     TIMESTAMP WITHOUT TIME ZONE        NOT NULL,
    allow_original BOOLEAN  DEFAULT FALSE             NOT NULL,
    revoked_at     TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT share_link_pkey PRIMARY KEY (id)
);

CREATE TABLE spatial_ref_sys
(
    srid      INTEGER NOT NULL,
    auth_name VARCHAR(256),
    auth_srid INTEGER,
    srtext    VARCHAR(2048),
    proj4text VARCHAR(2048),
    CONSTRAINT spatial_ref_sys_pkey PRIMARY KEY (srid)
);

CREATE TABLE user_session
(
    id         CHAR(36)                    DEFAULT gen_random_uuid() NOT NULL,
    user_id    CHAR(36)                                              NOT NULL,
    jwt_id     TEXT                                                  NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()             NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE                           NOT NULL,
    CONSTRAINT user_session_pkey PRIMARY KEY (id)
);

CREATE TABLE webhook_event
(
    id           CHAR(36) DEFAULT gen_random_uuid() NOT NULL,
    provider     TEXT                               NOT NULL,
    event_type   TEXT                               NOT NULL,
    external_id  TEXT                               NOT NULL,
    payload_json JSONB                              NOT NULL,
    processed    BOOLEAN  DEFAULT FALSE             NOT NULL,
    processed_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT webhook_event_pkey PRIMARY KEY (id)
);

ALTER TABLE app_user
    ADD CONSTRAINT app_user_email_key UNIQUE (email);

ALTER TABLE share_link
    ADD CONSTRAINT share_link_token_key UNIQUE (token);

CREATE INDEX evidence_captured_at_idx ON evidence (captured_at);

CREATE INDEX evidence_status_idx ON evidence (status);

ALTER TABLE coc_report
    ADD CONSTRAINT coc_report_evidence_id_fkey FOREIGN KEY (evidence_id) REFERENCES evidence (id) ON DELETE CASCADE;

ALTER TABLE custody_event
    ADD CONSTRAINT custody_event_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES app_user (id) ON DELETE NO ACTION;

ALTER TABLE custody_event
    ADD CONSTRAINT custody_event_evidence_id_fkey FOREIGN KEY (evidence_id) REFERENCES evidence (id) ON DELETE CASCADE;

CREATE INDEX custody_event_evidence_id_idx ON custody_event (evidence_id);

ALTER TABLE evidence
    ADD CONSTRAINT evidence_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES app_user (id) ON DELETE SET NULL;

CREATE INDEX evidence_owner_user_id_idx ON evidence (owner_user_id);

ALTER TABLE processing_job
    ADD CONSTRAINT processing_job_evidence_id_fkey FOREIGN KEY (evidence_id) REFERENCES evidence (id) ON DELETE CASCADE;

ALTER TABLE share_link
    ADD CONSTRAINT share_link_created_by_fkey FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE CASCADE;

ALTER TABLE share_link
    ADD CONSTRAINT share_link_evidence_id_fkey FOREIGN KEY (evidence_id) REFERENCES evidence (id) ON DELETE CASCADE;

ALTER TABLE user_session
    ADD CONSTRAINT user_session_user_id_fkey FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE;

CREATE VIEW geography_columns AS
SELECT current_database()               AS f_table_catalog,
       n.nspname                        AS f_table_schema,
       c.relname                        AS f_table_name,
       a.attname                        AS f_geography_column,
       postgis_typmod_dims(a.atttypmod) AS coord_dimension,
       postgis_typmod_srid(a.atttypmod) AS srid,
       postgis_typmod_type(a.atttypmod) AS type
FROM pg_class c,
     pg_attribute a,
     pg_type t,
     pg_namespace n
WHERE t.typname = 'geography'::name
  AND a.attisdropped = false
  AND a.atttypid = t.oid
  AND a.attrelid = c.oid
  AND c.relnamespace = n.oid
  AND (c.relkind = ANY (ARRAY ['r'::"char", 'v'::"char", 'm'::"char", 'f'::"char", 'p'::"char"]))
  AND NOT pg_is_other_temp_schema(c.relnamespace)
  AND has_table_privilege(c.oid, 'SELECT'::text);

alter table geography_columns
    owner to postgres;

grant select on geography_columns to public;

CREATE VIEW geometry_columns AS
SELECT current_database()::character varying(256)              AS f_table_catalog,
       n.nspname                                               AS f_table_schema,
       c.relname                                               AS f_table_name,
       a.attname                                               AS f_geometry_column,
       COALESCE(postgis_typmod_dims(a.atttypmod), sn.ndims, 2) AS coord_dimension,
       COALESCE(NULLIF(postgis_typmod_srid(a.atttypmod), 0), sr.srid,
                0)                                             AS srid,
       replace(replace(COALESCE(NULLIF(upper(postgis_typmod_type(a.atttypmod)), 'GEOMETRY'::text), st.type,
                                'GEOMETRY'::text), 'ZM'::text, ''::text), 'Z'::text,
               ''::text)::character varying(30)                AS type
FROM pg_class c
         JOIN pg_attribute a ON a.attrelid = c.oid AND NOT a.attisdropped
         JOIN pg_namespace n ON c.relnamespace = n.oid
         JOIN pg_type t ON a.atttypid = t.oid
         LEFT JOIN (SELECT s.connamespace,
                           s.conrelid,
                           s.conkey,
                           (regexp_match(s.consrc, 'geometrytype(w+)s*=s*(w+)'::text, 'i'::text))[1] AS type
                    FROM (SELECT pg_constraint.connamespace,
                                 pg_constraint.conrelid,
                                 pg_constraint.conkey,
                                 pg_get_constraintdef(pg_constraint.oid) AS consrc
                          FROM pg_constraint) s
                    WHERE s.consrc ~* 'geometrytype(w+)s*=s*w+'::text) st
                   ON st.connamespace = n.oid AND st.conrelid = c.oid AND (a.attnum = ANY (st.conkey))
         LEFT JOIN (SELECT s.connamespace,
                           s.conrelid,
                           s.conkey,
                           (regexp_match(s.consrc, 'ndims(w+)s*=s*(d+)'::text, 'i'::text))[1]::integer AS ndims
                    FROM (SELECT pg_constraint.connamespace,
                                 pg_constraint.conrelid,
                                 pg_constraint.conkey,
                                 pg_get_constraintdef(pg_constraint.oid) AS consrc
                          FROM pg_constraint) s
                    WHERE s.consrc ~* 'ndims(w+)s*=s*d+'::text) sn
                   ON sn.connamespace = n.oid AND sn.conrelid = c.oid AND (a.attnum = ANY (sn.conkey))
         LEFT JOIN (SELECT s.connamespace,
                           s.conrelid,
                           s.conkey,
                           (regexp_match(s.consrc, 'srid(w+)s*=s*(d+)'::text, 'i'::text))[1]::integer AS srid
                    FROM (SELECT pg_constraint.connamespace,
                                 pg_constraint.conrelid,
                                 pg_constraint.conkey,
                                 pg_get_constraintdef(pg_constraint.oid) AS consrc
                          FROM pg_constraint) s
                    WHERE s.consrc ~* 'srid(w+)s*=s*d+'::text) sr
                   ON sr.connamespace = n.oid AND sr.conrelid = c.oid AND (a.attnum = ANY (sr.conkey))
WHERE (c.relkind = ANY (ARRAY ['r'::"char", 'v'::"char", 'm'::"char", 'f'::"char", 'p'::"char"]))
  AND NOT c.relname = 'raster_columns'::name
  AND t.typname = 'geometry'::name
  AND NOT pg_is_other_temp_schema(c.relnamespace)
  AND has_table_privilege(c.oid, 'SELECT'::text);

alter table geometry_columns
    owner to postgres;

grant select on geometry_columns to public;