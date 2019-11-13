CREATE TYPE OAuth2Scope AS ENUM ('IDENTIFY', 'GUILD_JOIN');

CREATE TABLE oauth2
(
    user_id       BIGINT        NOT NULL,
    access_token  TEXT          NOT NULL,
    expires       timestamptz   NOT NULL,
    refresh_token TEXT          NOT NULL,
    scopes        OAuth2Scope[] NOT NULL,
    CONSTRAINT oauth2_pkey PRIMARY KEY (user_id)
);
