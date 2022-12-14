--liquibase formatted sql

--changeset usov:1

CREATE TABLE notification_task
(
    id      BIGSERIAL PRIMARY KEY,
    chat_id BIGINT,
    text    TEXT,
    date    TIMESTAMP

)