CREATE TABLE public.last_active
(
	user_id     bigint NOT NULL PRIMARY KEY,
	timestamp	bigint NOT NULL,
	expires     bigint NOT NULL
);
