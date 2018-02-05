///*
// * Copyright (C) 2017 Dennis Neufeld
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Affero General Public License as published
// * by the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU Affero General Public License for more details.
// *
// * You should have received a copy of the GNU Affero General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//
//package space.npstr.wolfia.db.migrations;
//
//import space.npstr.sqlsauce.DatabaseConnection;
//import space.npstr.sqlsauce.DatabaseException;
//import space.npstr.sqlsauce.entities.discord.DiscordGuild;
//import space.npstr.sqlsauce.migration.Migration;
//
//import javax.annotation.Nonnull;
//import javax.persistence.EntityManager;
//import java.util.List;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
///**
// * Created by napster on 29.11.17.
// */
//@SuppressWarnings("Duplicates")
//public class m00003EGuildToDiscordGuild extends Migration {
//    @Override
//    public void up(@Nonnull final DatabaseConnection databaseConnection) throws DatabaseException {
//        final EntityManager em = databaseConnection.getEntityManager();
//        try {
//            em.getTransaction().begin();
//
//
//            //join_timestamp populate default value -1 + not null
//            em.createNativeQuery("UPDATE public.guilds SET joined_timestamp = :joined WHERE public.guilds.joined_timestamp IS NULL")
//                    .setParameter("joined", -1)
//                    .executeUpdate();
//            em.createNativeQuery("ALTER TABLE public.guilds ALTER COLUMN joined_timestamp SET NOT NULL")
//                    .executeUpdate();
//
//            //left_timestamp populate default value -1 + not null
//            em.createNativeQuery("UPDATE public.guilds SET left_timestamp = :left WHERE public.guilds.left_timestamp IS NULL")
//                    .setParameter("left", -1)
//                    .executeUpdate();
//            em.createNativeQuery("ALTER TABLE public.guilds ALTER COLUMN left_timestamp SET NOT NULL")
//                    .executeUpdate();
//
//            //present populate default value false + not null
//            em.createNativeQuery("UPDATE public.guilds SET present = :present WHERE public.guilds.present IS NULL")
//                    .setParameter("present", false)
//                    .executeUpdate();
//            em.createNativeQuery("ALTER TABLE public.guilds ALTER COLUMN present SET NOT NULL")
//                    .executeUpdate();
//
//            //name populate default value + not null
//            em.createNativeQuery("UPDATE public.guilds SET name = :unknownname WHERE public.guilds.name IS NULL")
//                    .setParameter("unknownname", DiscordGuild.UNKNOWN_NAME)
//                    .executeUpdate();
//            em.createNativeQuery("ALTER TABLE public.guilds ALTER COLUMN name SET NOT NULL")
//                    .executeUpdate();
//
//
//            final List<Long> guildIds = em.createQuery("SELECT Guild.guildId FROM EGuild Guild", Long.class)
//                    .getResultList();
//
//            //language=PostgreSQL
//            final String avatarUrlQuery = "SELECT public.guilds.avatar_url FROM public.guilds WHERE public.guilds.guild_id = :guildid";
//            //language=PostgreSQL
//            final String setIconIdQuery = "UPDATE public.guilds SET icon_id = :iconid WHERE public.guilds.guild_id = :guildid";
//
//            //https://regex101.com/r/XX1myZ/2
//            final Pattern iconIdRegex = Pattern.compile("^.*icons/[0-9]+/([a-zA-Z0-9]+)\\..+$");
//
//
//            for (final long guildId : guildIds) {
//                final String avatarUrl = (String) em.createNativeQuery(avatarUrlQuery)
//                        .setParameter("guildid", guildId)
//                        .getResultList().get(0);
//                final Matcher m = iconIdRegex.matcher(avatarUrl);
//                if (m.matches()) {
//                    final String iconId = m.group(1);
//                    em.createNativeQuery(setIconIdQuery)
//                            .setParameter("iconid", iconId)
//                            .setParameter("guildid", guildId)
//                            .executeUpdate();
//                }
//            }
//
//            //dont delete. deleting things is easy. we can do that from the admin console after making sure the data was
//            // transfered safe and sound
//            //queries.add("ALTER TABLE public.guilds DROP COLUMN avatar_url");
//
//            em.getTransaction().commit();
//        } finally {
//            em.close();
//        }
//    }
//}
