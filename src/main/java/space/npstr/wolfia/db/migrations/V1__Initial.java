/*
 * Copyright (C) 2017-2018 Dennis Neufeld
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

package space.npstr.wolfia.db.migrations;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Created by napster on 26.04.18.
 */
public class V1__Initial implements JdbcMigration {

    //language=PostgreSQL
    private static final String CREATE_TABLE_HSTOREX
            = "CREATE TABLE IF NOT EXISTS public.hstorex "
            + "( "
            + "     name     text COLLATE pg_catalog.\"default\" NOT NULL, "
            + "     hstorex  hstore NOT NULL, "
            + "     CONSTRAINT hstorex_pkey PRIMARY KEY (name) "
            + ");";

    //language=PostgreSQL
    private static final String CREATE_TABLE_BANLIST
            = "CREATE TABLE IF NOT EXISTS public.ban "
            + "( "
            + "     user_id      bigint NOT NULL, "
            + "     scope        text COLLATE pg_catalog.\"default\" NOT NULL, "
            + "     CONSTRAINT ban_pkey PRIMARY KEY (user_id) "
            + ");";

    //language=PostgreSQL
    private static final String CREATE_TABLE_CACHED_GUILD
            = "CREATE TABLE IF NOT EXISTS public.cached_guild "
            + "( "
            + "     guild_id                bigint NOT NULL, "
            + "     joined_timestamp        bigint NOT NULL, "
            + "     left_timestamp          bigint NOT NULL, "
            + "     present                 boolean NOT NULL, "
            + "     name                    text COLLATE pg_catalog.\"default\" NOT NULL DEFAULT 'Unknown Guild'::text, "
            + "     afk_channel_id          bigint, "
            + "     afk_timeout_seconds     integer NOT NULL DEFAULT 300, "
            + "     explicit_content_level  integer NOT NULL DEFAULT '-1'::integer, "
            + "     icon_id                 text COLLATE pg_catalog.\"default\", "
            + "     mfa_level               integer NOT NULL DEFAULT '-1'::integer, "
            + "     notification_level      integer NOT NULL DEFAULT '-1'::integer, "
            + "     owner_id                bigint NOT NULL DEFAULT '-1'::integer, "
            + "     region                  text COLLATE pg_catalog.\"default\" NOT NULL DEFAULT ''::text, "
            + "     splash_id               text COLLATE pg_catalog.\"default\", "
            + "     system_channel_id       bigint, "
            + "     verification_level      integer NOT NULL DEFAULT '-1'::integer, "
            + "     CONSTRAINT cached_guild_pkey PRIMARY KEY (guild_id) "
            + ");";

    //language=PostgreSQL
    private static final String CREATE_TABLE_CACHED_USER
            = "CREATE TABLE IF NOT EXISTS public.cached_user "
            + "( "
            + "     user_id         bigint NOT NULL, "
            + "     name            text COLLATE pg_catalog.\"default\" NOT NULL, "
            + "     nicks           hstore NOT NULL, "
            + "     avatar_id       text COLLATE pg_catalog.\"default\", "
            + "     bot             boolean NOT NULL DEFAULT false, "
            + "     discriminator   smallint NOT NULL DEFAULT '-1'::integer, "
            + "     CONSTRAINT cached_user_pkey PRIMARY KEY (user_id) "
            + ");";

    //language=PostgreSQL
    private static final String CREATE_TABLE_CHANNEL_SETTINGS
            = "CREATE TABLE IF NOT EXISTS public.channel_settings "
            + "( "
            + "     channel_id          bigint NOT NULL, "
            + "     access_role_id      bigint NOT NULL DEFAULT '-1'::bigint, "
            + "     tag_cooldown        bigint NOT NULL DEFAULT 5, "
            + "     tag_last_used       bigint NOT NULL DEFAULT 0, "
            + "     tags                bigint[] NOT NULL DEFAULT ARRAY[]::bigint[], "
            + "     CONSTRAINT channel_settings_pkey PRIMARY KEY (channel_id) "
            + ");";

    //language=PostgreSQL
    private static final String CREATE_TABLE_PRIVATE_GUILD
            = "CREATE TABLE IF NOT EXISTS public.private_guild "
            + "( "
            + "     guild_id    bigint NOT NULL, "
            + "     nr          integer NOT NULL, "
            + "     CONSTRAINT private_guild_pkey          PRIMARY KEY (guild_id), "
            + "     CONSTRAINT private_guild_number_unique  UNIQUE (nr) "
            + ");";

    //language=PostgreSQL
    private static final String CREATE_TABLE_SETUP
            = "CREATE TABLE IF NOT EXISTS public.setup "
            + "( "
            + "     channel_id      bigint NOT NULL, "
            + "     game            text COLLATE pg_catalog.\"default\" NOT NULL, "
            + "     mode            text COLLATE pg_catalog.\"default\" NOT NULL, "
            + "     day_length      bigint NOT NULL DEFAULT 600000, "
            + "     inned_users     bigint[] NOT NULL DEFAULT ARRAY[]::bigint[], "
            + "     CONSTRAINT setup_pkey PRIMARY KEY (channel_id) "
            + ");";

    //language=PostgreSQL
    private static final String CREATE_TABLE_STATS_GAME
            = "CREATE TABLE IF NOT EXISTS public.stats_game "
            + "( "
            // Using bigserial will automatically create a sequence called stats_game_game_id_seq with
            // INCREMENT 1 START 1 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1
            // and assign the nextval() of it as the default value for the column
            // (similar to the manual assignment of the stats_id_seq for the other stats tables)
            // This is considered good enough for incrementing game numbers, even though sometimes numbers might be skipped.
            + "     game_id         bigserial NOT NULL, "
            + "     channel_id      bigint NOT NULL, "
            + "     channel_name    text COLLATE pg_catalog.\"default\" NOT NULL, "
            + "     end_time        bigint NOT NULL, "
            + "     game_mode            text COLLATE pg_catalog.\"default\" NOT NULL, "
            + "     game_type            text COLLATE pg_catalog.\"default\" NOT NULL, "
            + "     guild_id        bigint NOT NULL, "
            + "     guild_name      text COLLATE pg_catalog.\"default\" NOT NULL, "
            + "     player_size     integer NOT NULL, "
            + "     start_time      bigint NOT NULL, "
            + "     CONSTRAINT stats_game_pkey PRIMARY KEY (game_id) "
            + ");";

    // This sequence is used by the stats tables defined below.
    // If building the schema from scratch I would have used individual sequences for each table, but at the moment of
    // replacing hibernate auto-ddl with flyway we already created almost 2 million entries with a shared sequence,
    // and I don't feel comfortable changing that now. -Napster
    //language=PostgreSQL
    private static final String CREATE_SEQUENCE_STATS_ID
            = "CREATE SEQUENCE IF NOT EXISTS public.stats_id_seq "
            + "     INCREMENT 1 "
            + "     START 1 "
            + "     MINVALUE 1 "
            + "     MAXVALUE 9223372036854775807 "
            + "     CACHE 1;";

    //language=PostgreSQL
    private static final String CREATE_TABLE_STATS_ACTION
            = "CREATE TABLE IF NOT EXISTS public.stats_action "
            + "( "
            + "     action_id       bigint NOT NULL DEFAULT nextval('stats_id_seq'::regclass), "
            + "     action_type     text COLLATE pg_catalog.\"default\" NOT NULL, "
            + "     actor           bigint NOT NULL, "
            + "     cycle           integer NOT NULL, "
            + "     sequence        integer NOT NULL, "
            + "     phase           text COLLATE pg_catalog.\"default\" NOT NULL, "
            + "     target          bigint NOT NULL, "
            + "     happened        bigint NOT NULL, "
            + "     submitted       bigint NOT NULL, "
            + "     game_id         bigint NOT NULL, "
            + "     additional_info text COLLATE pg_catalog.\"default\", "
            + "     CONSTRAINT stats_action_pkey PRIMARY KEY (action_id), "
            + "     CONSTRAINT stats_action_game_id_fkey FOREIGN KEY (game_id) "
            + "         REFERENCES public.stats_game (game_id) MATCH SIMPLE "
            + "         ON UPDATE NO ACTION "
            + "         ON DELETE NO ACTION "
            + ");";

    //language=PostgreSQL
    private static final String CREATE_TABLE_STATS_TEAM
            = "CREATE TABLE IF NOT EXISTS public.stats_team "
            + "( "
            + "     team_id         bigint NOT NULL DEFAULT nextval('stats_id_seq'::regclass), "
            + "     alignment       text COLLATE pg_catalog.\"default\" NOT NULL, "
            + "     is_winner       boolean NOT NULL, "
            + "     name            text COLLATE pg_catalog.\"default\" NOT NULL, "
            + "     team_size       integer NOT NULL, "
            + "     game_id         bigint NOT NULL, "
            + "     CONSTRAINT stats_team_pkey PRIMARY KEY (team_id), "
            + "     CONSTRAINT stats_team_game_id_fkey FOREIGN KEY (game_id) "
            + "         REFERENCES public.stats_game (game_id) MATCH SIMPLE "
            + "         ON UPDATE NO ACTION "
            + "         ON DELETE NO ACTION "
            + ");";

    //language=PostgreSQL
    private static final String CREATE_TABLE_STATS_PLAYER
            = "CREATE TABLE IF NOT EXISTS public.stats_player "
            + "( "
            + "     player_id       bigint NOT NULL DEFAULT nextval('stats_id_seq'::regclass), "
            + "     alignment       text COLLATE pg_catalog.\"default\" NOT NULL, "
            + "     nickname        text COLLATE pg_catalog.\"default\", "
            + "     role            text COLLATE pg_catalog.\"default\" NOT NULL, "
            + "     total_postlength integer NOT NULL , "
            + "     total_posts     integer NOT NULL, "
            + "     user_id         bigint NOT NULL, "
            + "     team_id         bigint NOT NULL, "
            + "     CONSTRAINT stats_player_pkey PRIMARY KEY (player_id), "
            + "     CONSTRAINT stats_player_team_id_fkey FOREIGN KEY (team_id) "
            + "         REFERENCES public.stats_team (team_id) MATCH SIMPLE "
            + "         ON UPDATE NO ACTION "
            + "         ON DELETE NO ACTION "
            + ");";


    @Override
    public void migrate(final Connection connection) throws Exception {
        try (final Statement st = connection.createStatement()) {
            st.execute(CREATE_TABLE_HSTOREX);
            st.execute(CREATE_TABLE_BANLIST);
            st.execute(CREATE_TABLE_CACHED_GUILD);
            st.execute(CREATE_TABLE_CACHED_USER);
            st.execute(CREATE_TABLE_CHANNEL_SETTINGS);
            st.execute(CREATE_TABLE_PRIVATE_GUILD);
            st.execute(CREATE_TABLE_SETUP);
            st.execute(CREATE_TABLE_STATS_GAME);
            st.execute(CREATE_SEQUENCE_STATS_ID);
            st.execute(CREATE_TABLE_STATS_ACTION);
            st.execute(CREATE_TABLE_STATS_TEAM);
            st.execute(CREATE_TABLE_STATS_PLAYER);
        }
    }
}
