CREATE TABLE app_user
(
    id            UUID                        NOT NULL,
    email         VARCHAR(255)                NOT NULL,
    password_hash VARCHAR(255)                NOT NULL,
    display_name  VARCHAR(255),
    role          VARCHAR(255)                NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_app_user PRIMARY KEY (id)
);

CREATE TABLE coc_report
(
    id          UUID                        NOT NULL,
    evidence_id UUID                        NOT NULL,
    pdf_key     VARCHAR(255)                NOT NULL,
    sha256      VARCHAR(255)                NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_coc_report PRIMARY KEY (id)
);

CREATE TABLE custody_event
(
    id            UUID                        NOT NULL,
    evidence_id   UUID                        NOT NULL,
    actor_user_id UUID,
    event_type    VARCHAR(255)                NOT NULL,
    meta_json     JSONB,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_custody_event PRIMARY KEY (id)
);

CREATE TABLE evidence
(
    id                 UUID                        NOT NULL,
    owner_user_id      UUID,
    title              VARCHAR(255),
    description        VARCHAR(255),
    captured_at        TIMESTAMP WITHOUT TIME ZONE,
    capture_latlon     GEOGRAPHY(Point, 4326),
    capture_accuracy_m DECIMAL,
    status             VARCHAR(255)                NOT NULL,
    original_sha256    VARCHAR(255)                NOT NULL,
    original_size_b    BIGINT                      NOT NULL,
    original_key       VARCHAR(255)                NOT NULL,
    derivative_key     VARCHAR(255),
    thumbnail_key      VARCHAR(255),
    redacted_key       VARCHAR(255),
    redacted_size      BIGINT,
    legal_hold         BOOLEAN                     NOT NULL,
    created_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_evidence PRIMARY KEY (id)
);

CREATE TABLE processing_job
(
    id           UUID                        NOT NULL,
    evidence_id  UUID                        NOT NULL,
    type         VARCHAR(255)                NOT NULL,
    status       VARCHAR(255)                NOT NULL,
    attempts     INTEGER                     NOT NULL,
    error_msg    TEXT,
    payload_json JSONB,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_processing_job PRIMARY KEY (id)
);

CREATE TABLE share_link
(
    id             UUID                        NOT NULL,
    evidence_id    UUID                        NOT NULL,
    created_by     UUID                        NOT NULL,
    token          VARCHAR(255)                NOT NULL,
    expires_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    allow_original BOOLEAN                     NOT NULL,
    revoked_at     TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_share_link PRIMARY KEY (id)
);

CREATE TABLE user_session
(
    id         UUID                        NOT NULL,
    user_id    UUID                        NOT NULL,
    jwt_id     VARCHAR(255)                NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_user_session PRIMARY KEY (id)
);

CREATE TABLE webhook_event
(
    id           UUID         NOT NULL,
    provider     VARCHAR(255) NOT NULL,
    event_type   VARCHAR(255) NOT NULL,
    external_id  VARCHAR(255) NOT NULL,
    payload_json JSONB        NOT NULL,
    processed    BOOLEAN      NOT NULL,
    processed_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_webhook_event PRIMARY KEY (id)
);

ALTER TABLE app_user
    ADD CONSTRAINT uc_app_user_email UNIQUE (email);

ALTER TABLE share_link
    ADD CONSTRAINT uc_share_link_token UNIQUE (token);

ALTER TABLE coc_report
    ADD CONSTRAINT FK_COC_REPORT_ON_EVIDENCE FOREIGN KEY (evidence_id) REFERENCES evidence (id);

ALTER TABLE custody_event
    ADD CONSTRAINT FK_CUSTODY_EVENT_ON_ACTOR_USER FOREIGN KEY (actor_user_id) REFERENCES app_user (id);

ALTER TABLE custody_event
    ADD CONSTRAINT FK_CUSTODY_EVENT_ON_EVIDENCE FOREIGN KEY (evidence_id) REFERENCES evidence (id);

ALTER TABLE evidence
    ADD CONSTRAINT FK_EVIDENCE_ON_OWNER_USER FOREIGN KEY (owner_user_id) REFERENCES app_user (id);

ALTER TABLE processing_job
    ADD CONSTRAINT FK_PROCESSING_JOB_ON_EVIDENCE FOREIGN KEY (evidence_id) REFERENCES evidence (id);

ALTER TABLE share_link
    ADD CONSTRAINT FK_SHARE_LINK_ON_CREATED_BY FOREIGN KEY (created_by) REFERENCES app_user (id);

ALTER TABLE share_link
    ADD CONSTRAINT FK_SHARE_LINK_ON_EVIDENCE FOREIGN KEY (evidence_id) REFERENCES evidence (id);

ALTER TABLE user_session
    ADD CONSTRAINT FK_USER_SESSION_ON_USER FOREIGN KEY (user_id) REFERENCES app_user (id);