-- The idea here is that settings are optional, and should therefore be allowed to be NULL.
-- The database schema is just not a good place to put defaults in for tables that represent settings.
-- Just because the user sets one setting to a specific value, we should not automatically insert all other defaults
-- into a settings table row.
-- Maybe we want to change defaults in the future in the application code? We won't be able to tell which
-- settings were actually consciously set by a user, and which ones are just a result of table defaults being inserted.


-- guild settings
ALTER TABLE public.guild_settings
    ALTER COLUMN name DROP NOT NULL;
ALTER TABLE public.guild_settings
    ALTER COLUMN name DROP DEFAULT;

UPDATE public.guild_settings
SET name = NULL
WHERE name = 'Unknown Guild';

-- clean up the rows that have default values only
DELETE
FROM public.guild_settings
WHERE name IS NULL
  AND icon_id IS NULL;


-- channel settings
ALTER TABLE public.channel_settings
    ALTER COLUMN access_role_id DROP NOT NULL;
ALTER TABLE public.channel_settings
    ALTER COLUMN access_role_id DROP DEFAULT;

UPDATE public.channel_settings
SET access_role_id = NULL
WHERE access_role_id = -1;


ALTER TABLE public.channel_settings
    ALTER COLUMN tag_cooldown DROP NOT NULL;
ALTER TABLE public.channel_settings
    ALTER COLUMN tag_cooldown DROP DEFAULT;

UPDATE public.channel_settings
SET tag_cooldown = NULL
WHERE tag_cooldown = 5;

-- consolidate the two former defaults
UPDATE public.channel_settings
SET tag_last_used = 0
WHERE tag_last_used = -1;

-- clean up the rows that have default values only
DELETE
FROM public.channel_settings
WHERE access_role_id IS NULL
  AND tag_cooldown IS NULL
  AND tag_last_used = 0
  AND tags = '{}';
