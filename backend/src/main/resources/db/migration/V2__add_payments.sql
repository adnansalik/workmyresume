-- V2: Payment orders for the Razorpay checkout lifecycle.

CREATE TABLE payments (
    id bigint NOT NULL,
    created_at timestamp(6) with time zone,
    amount bigint,
    currency character varying(10),
    razorpay_order_id character varying(64) NOT NULL,
    status character varying(32) NOT NULL,
    verified_at timestamp(6) with time zone,
    user_id bigint NOT NULL
);

CREATE SEQUENCE payments_seq START WITH 1 INCREMENT BY 50 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER TABLE ONLY payments ADD CONSTRAINT payments_pkey PRIMARY KEY (id);
ALTER TABLE ONLY payments ADD CONSTRAINT uk_razorpay_order_id UNIQUE (razorpay_order_id);
ALTER TABLE ONLY payments
    ADD CONSTRAINT fk_payments_user FOREIGN KEY (user_id) REFERENCES users (id);