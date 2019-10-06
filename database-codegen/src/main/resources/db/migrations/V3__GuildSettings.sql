-- we're only actually interested in name and icon
ALTER TABLE public.cached_guild
    DROP joined_timestamp,
    DROP left_timestamp,
    DROP present,
    DROP afk_channel_id,
    DROP afk_timeout_seconds,
    DROP explicit_content_level,
    DROP mfa_level,
    DROP notification_level,
    DROP owner_id,
    DROP region,
    DROP splash_id,
    DROP system_channel_id,
    DROP verification_level;

ALTER TABLE public.cached_guild
    RENAME TO guild_settings;

ALTER INDEX public.cached_guild_pkey
    RENAME TO guild_settings_pkey;
