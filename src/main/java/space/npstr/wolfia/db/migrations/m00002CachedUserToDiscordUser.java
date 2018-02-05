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
//import space.npstr.sqlsauce.entities.discord.DiscordUser;
//import space.npstr.sqlsauce.migration.Migration;
//
//import javax.annotation.Nonnull;
//import javax.persistence.EntityManager;
//import java.util.List;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
///**
// * Created by napster on 17.11.17.
// */
//@SuppressWarnings("Duplicates")
//public class m00002CachedUserToDiscordUser extends Migration {
//    @Override
//    public void up(@Nonnull final DatabaseConnection databaseConnection) throws DatabaseException {
//
//        final EntityManager em = databaseConnection.getEntityManager();
//        try {
//            em.getTransaction().begin();
//            em.createNativeQuery("UPDATE public.cached_users SET name = :unknownname WHERE public.cached_users.name IS NULL")
//                    .setParameter("unknownname", DiscordUser.UNKNOWN_NAME)
//                    .executeUpdate();
//            em.createNativeQuery("ALTER TABLE public.cached_users ALTER COLUMN name SET NOT NULL")
//                    .executeUpdate();
//
//
//            final List<Long> userIds = em.createQuery("SELECT User.userId FROM CachedUser User", Long.class)
//                    .getResultList();
//
//            //language=PostgreSQL
//            final String avatarUrlQuery = "SELECT public.cached_users.avatar_url FROM public.cached_users WHERE public.cached_users.user_id = :userid";
//            //language=PostgreSQL
//            final String setAvatarIdQuery = "UPDATE public.cached_users SET avatar_id = :avatarid WHERE public.cached_users.user_id = :userid";
//
//            //https://regex101.com/r/SqDksz/2
//            final Pattern avatarIdRegex = Pattern.compile("^.*avatars/[0-9]+/([a-zA-Z0-9]+)\\..+$");
//
//            for (final long userId : userIds) {
//                final String avatarUrl = (String) em.createNativeQuery(avatarUrlQuery)
//                        .setParameter("userid", userId)
//                        .getResultList().get(0);
//                final Matcher m = avatarIdRegex.matcher(avatarUrl);
//                if (m.matches()) {
//                    final String avatarId = m.group(1);
//                    em.createNativeQuery(setAvatarIdQuery)
//                            .setParameter("avatarid", avatarId)
//                            .setParameter("userid", userId)
//                            .executeUpdate();
//                }
//            }
//
//            //dont delete. deleting things is easy. we can do that from the admin console after making sure the data was
//            // transfered safe and sound
//            //queries.add("ALTER TABLE public.cached_users DROP COLUMN avatar_url");
//            em.getTransaction().commit();
//        } finally {
//            em.close();
//        }
//    }
//}
