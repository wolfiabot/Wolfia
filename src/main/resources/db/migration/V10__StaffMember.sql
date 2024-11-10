CREATE TYPE staff_function AS ENUM ('DEVELOPER', 'MODERATOR', 'SETUP_MANAGER');

CREATE TABLE staff_member
(
    user_id  BIGINT         NOT NULL,
    function staff_function NOT NULL,
    slogan   TEXT,
    link     TEXT,
    active   BOOLEAN        NOT NULL DEFAULT true,  -- staff that lose their staff role are marked inactive
    enabled  BOOLEAN        NOT NULL DEFAULT false, -- staff need to opt-in to be displayed
    CONSTRAINT staff_member_pkey PRIMARY KEY (user_id)
);
