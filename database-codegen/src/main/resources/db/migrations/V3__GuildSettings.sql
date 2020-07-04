/*
 * Copyright (C) 2016-2020 the original author or authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
