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
