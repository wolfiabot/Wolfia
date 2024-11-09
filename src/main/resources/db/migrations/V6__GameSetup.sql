ALTER TABLE public.setup
    ALTER COLUMN game DROP NOT NULL;

UPDATE public.setup
SET game = NULL
WHERE game = 'POPCORN';

ALTER TABLE public.setup
    ALTER COLUMN mode DROP NOT NULL;

UPDATE public.setup
SET mode = NULL
WHERE mode = 'WILD';

ALTER TABLE public.setup
    ALTER COLUMN day_length DROP NOT NULL;
ALTER TABLE public.setup
    ALTER COLUMN day_length DROP DEFAULT;

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

-- consolidate the two former defaults
UPDATE public.setup
SET day_length = 300000
WHERE day_length = 600000;

UPDATE public.setup
SET day_length = NULL
WHERE day_length = 300000;

DELETE
FROM public.setup
WHERE game IS NULL
  AND mode IS NULL
  AND day_length IS NULL
  AND inned_users = '{}';


ALTER TABLE public.setup
    RENAME TO game_setup;
