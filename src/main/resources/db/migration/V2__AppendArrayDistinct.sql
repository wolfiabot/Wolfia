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

-- Merge two arrays, but only keep distinct values.
CREATE OR REPLACE FUNCTION array_append_distinct(array1 anyarray, array2 anyarray)
    RETURNS anyarray AS
$$
SELECT ARRAY(SELECT unnest(array1) UNION SELECT unnest(array2))
$$ LANGUAGE SQL IMMUTABLE;



-- Remove array2 elements from array1
CREATE OR REPLACE FUNCTION array_diff(array1 anyarray, array2 anyarray)
    RETURNS anyarray AS
$$
SELECT COALESCE(array_agg(elem), '{}')
FROM unnest(array1) elem
WHERE elem <> ALL (array2)
$$ LANGUAGE SQL IMMUTABLE;
