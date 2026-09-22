-- V1: Initial schema (mirrors what Hibernate generated at bootstrap).

CREATE TABLE users (
    id bigint NOT NULL,
    created_at timestamp(6) with time zone,
    email character varying(255) NOT NULL,
    is_active boolean,
    password character varying(255) NOT NULL,
    role character varying(255) DEFAULT 'ROLE_USER'::character varying NOT NULL,
    plan character varying(255) DEFAULT 'FREE'::character varying NOT NULL
);

CREATE SEQUENCE users_seq START WITH 1 INCREMENT BY 50 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE resumes (
    id bigint NOT NULL,
    created_at timestamp(6) with time zone,
    title character varying(255),
    user_id bigint NOT NULL
);

CREATE SEQUENCE resumes_seq START WITH 1 INCREMENT BY 50 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE resume_versions (
    id bigint NOT NULL,
    created_at timestamp(6) with time zone,
    raw_text text,
    title character varying(255),
    version_number integer NOT NULL,
    resume_id bigint NOT NULL
);

CREATE SEQUENCE resume_versions_seq START WITH 1 INCREMENT BY 50 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE resume_files (
    id bigint NOT NULL,
    content_type character varying(255),
    original_filename character varying(255) NOT NULL,
    size_bytes bigint,
    storage_key character varying(255),
    uploaded_at timestamp(6) with time zone,
    version_id bigint NOT NULL
);

CREATE SEQUENCE resume_files_seq START WITH 1 INCREMENT BY 50 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE resume_analyses (
    id bigint NOT NULL,
    created_at timestamp(6) with time zone,
    gaps jsonb,
    job_description text,
    score integer,
    strengths jsonb,
    suggestions jsonb,
    version_id bigint NOT NULL
);

CREATE SEQUENCE resume_analyses_seq START WITH 1 INCREMENT BY 50 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE subscriptions (
    id bigint NOT NULL,
    canceled_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone,
    current_period_end timestamp(6) with time zone NOT NULL,
    current_period_start timestamp(6) with time zone NOT NULL,
    plan character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    user_id bigint NOT NULL
);

CREATE SEQUENCE subscriptions_seq START WITH 1 INCREMENT BY 50 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER TABLE ONLY users ADD CONSTRAINT users_pkey PRIMARY KEY (id);
ALTER TABLE ONLY users ADD CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email);
ALTER TABLE ONLY resumes ADD CONSTRAINT resumes_pkey PRIMARY KEY (id);
ALTER TABLE ONLY resume_versions ADD CONSTRAINT resume_versions_pkey PRIMARY KEY (id);
ALTER TABLE ONLY resume_files ADD CONSTRAINT resume_files_pkey PRIMARY KEY (id);
ALTER TABLE ONLY resume_analyses ADD CONSTRAINT resume_analyses_pkey PRIMARY KEY (id);
ALTER TABLE ONLY subscriptions ADD CONSTRAINT subscriptions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY resumes
    ADD CONSTRAINT fk340nuaivxiy99hslr3sdydfvv FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE ONLY resume_versions
    ADD CONSTRAINT fk2tc1ij6lmop8569p74kl2u2ut FOREIGN KEY (resume_id) REFERENCES resumes (id);
ALTER TABLE ONLY resume_files
    ADD CONSTRAINT fkoj4hrry7kc1io0hdyvpvylyfw FOREIGN KEY (version_id) REFERENCES resume_versions (id);
ALTER TABLE ONLY resume_analyses
    ADD CONSTRAINT fk4rkc7tuyy9r4hxwn9d27htcec FOREIGN KEY (version_id) REFERENCES resume_versions (id);
ALTER TABLE ONLY subscriptions
    ADD CONSTRAINT fkhro52ohfqfbay9774bev0qinr FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE ONLY subscriptions
    ADD CONSTRAINT subscriptions_plan_check
        CHECK (((plan)::text = ANY ((ARRAY['FREE'::character varying, 'PRO'::character varying])::text[])));
ALTER TABLE ONLY subscriptions
    ADD CONSTRAINT subscriptions_status_check
        CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'CANCELED'::character varying, 'EXPIRED'::character varying])::text[])));