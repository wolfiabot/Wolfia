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

ALTER TABLE public.private_guild
    RENAME CONSTRAINT private_guild_pkey TO private_room_pkey;
ALTER TABLE public.private_guild
    RENAME CONSTRAINT private_guild_number_unique TO private_room_number_unique;

ALTER TABLE public.private_guild
    RENAME TO private_room;

ALTER INDEX IF EXISTS public.private_guild_pkey
    RENAME TO private_room_pkey;
