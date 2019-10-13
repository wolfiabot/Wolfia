ALTER TABLE public.private_guild
    RENAME CONSTRAINT private_guild_pkey TO private_room_pkey;
ALTER TABLE public.private_guild
    RENAME CONSTRAINT private_guild_number_unique TO private_room_number_unique;

ALTER TABLE public.private_guild
    RENAME TO private_room;

ALTER INDEX IF EXISTS public.private_guild_pkey
    RENAME TO private_room_pkey;
