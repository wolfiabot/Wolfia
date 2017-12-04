/*
 * Copyright (C) 2017 Dennis Neufeld
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

import space.npstr.sqlsauce.DatabaseConnection;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.sqlsauce.migration.Migration;

import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by napster on 12.11.17.
 */
public class m00001FixCharacterVaryingColumns extends Migration {

    @Override
    public void up(@Nonnull final DatabaseConnection databaseConnection) throws DatabaseException {

        final List<String> queries = new ArrayList<>();

        //language=PostgreSQL
        queries.add("ALTER TABLE public.banlist ALTER COLUMN scope TYPE TEXT");

        //language=PostgreSQL
        queries.add("ALTER TABLE public.cached_users ALTER COLUMN name TYPE TEXT");
        //language=PostgreSQL
        queries.add("ALTER TABLE public.cached_users ALTER COLUMN avatar_url TYPE TEXT");

        //language=PostgreSQL
        queries.add("ALTER TABLE public.guilds ALTER COLUMN avatar_url TYPE TEXT");
        //language=PostgreSQL
        queries.add("ALTER TABLE public.guilds ALTER COLUMN name TYPE TEXT");

        //language=PostgreSQL
        queries.add("ALTER TABLE public.hstorex ALTER COLUMN name TYPE TEXT");

        //language=PostgreSQL
        queries.add("ALTER TABLE public.setups ALTER COLUMN game TYPE TEXT");
        //language=PostgreSQL
        queries.add("ALTER TABLE public.setups ALTER COLUMN mode TYPE TEXT");

        //language=PostgreSQL
        queries.add("ALTER TABLE public.stats_action ALTER COLUMN action_type TYPE TEXT");
        //language=PostgreSQL
        queries.add("ALTER TABLE public.stats_action ALTER COLUMN phase TYPE TEXT");

        //language=PostgreSQL
        queries.add("ALTER TABLE public.stats_commands ALTER COLUMN command_class TYPE TEXT");

        //language=PostgreSQL
        queries.add("ALTER TABLE public.stats_game ALTER COLUMN channel_name TYPE TEXT");
        //language=PostgreSQL
        queries.add("ALTER TABLE public.stats_game ALTER COLUMN game_mode TYPE TEXT");
        //language=PostgreSQL
        queries.add("ALTER TABLE public.stats_game ALTER COLUMN game_type TYPE TEXT");
        //language=PostgreSQL
        queries.add("ALTER TABLE public.stats_game ALTER COLUMN guild_name TYPE TEXT");

        //language=PostgreSQL
        queries.add("ALTER TABLE public.stats_player ALTER COLUMN nickname TYPE TEXT");
        //language=PostgreSQL
        queries.add("ALTER TABLE public.stats_player ALTER COLUMN role TYPE TEXT");
        //language=PostgreSQL
        queries.add("ALTER TABLE public.stats_player ALTER COLUMN alignment TYPE TEXT");

        //language=PostgreSQL
        queries.add("ALTER TABLE public.stats_team ALTER COLUMN alignment TYPE TEXT");
        //language=PostgreSQL
        queries.add("ALTER TABLE public.stats_team ALTER COLUMN name TYPE TEXT");


        final EntityManager entityManager = databaseConnection.getEntityManager();
        try {
            entityManager.getTransaction().begin();
            queries.forEach(q -> entityManager.createNativeQuery(q).executeUpdate());
            entityManager.getTransaction().commit();
        } finally {
            entityManager.close();
        }
    }
}
