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

package space.npstr.wolfia.db;

import org.hibernate.exception.JDBCConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.db.entity.SetupEntity;
import space.npstr.wolfia.db.entity.stats.GameStats;
import space.npstr.wolfia.db.entity.stats.TeamStats;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import java.util.List;

/**
 * Created by napster on 30.05.17.
 * <p>
 * This class is all about saving/loading/deleting entities
 * <p>
 * Some general notes: the entities/objects in Wolfia are mostly treated just as data containers, conveniently
 * describing what data we have and how exactly that data looks like. With Hibernate we automatically create the needed
 * columns, even if we later on decided to add more, we won't have to do any kind of annoying/complicated/errorprone
 * /timeconsuming lowlevel database operations. This is all about just throwing data at the database, easily getting it
 * out again, and being easily extendable in the future.
 * This is NOT about transfering actual objects between independent applications.
 */
public class DbWrapper {

    private static final Logger log = LoggerFactory.getLogger(DbWrapper.class);


    //########## saving

    public static void merge(final IEntity entity) {
        final DbManager dbManager = Wolfia.dbManager;
        final EntityManager em = dbManager.getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(entity);
            em.getTransaction().commit();
        } catch (final JDBCConnectionException e) {
            log.error("Failed to merge entity {}", entity, e);
            throw new RuntimeException(e);
        } finally {
            em.close();
        }
    }

    public static void persist(final Object object) {
        final DbManager dbManager = Wolfia.dbManager;
        final EntityManager em = dbManager.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(object);
            em.getTransaction().commit();
        } catch (final JDBCConnectionException e) {
            log.error("Failed to merge object {}", object, e);
            throw new RuntimeException(e);
        } finally {
            em.close();
        }
    }

    //########## loading

    //never returns null; IEntity objects are required to have a default constructor that sets them up with sensible
    // defaults, as long as we make sure that the id is a natural one (for example derived from a snowflake from
    // discord, aka channels, guilds, users etc)
    public static <E extends IEntity> E getEntity(final long id, final Class<E> clazz) {
        E entity = getObject(id, clazz);
        //return a fresh object if we didn't find the one we were looking for
        if (entity == null) entity = newInstance(id, clazz);
        return entity;
    }

    public static GameStats loadSingleGameStats(final long id) {
        final DbManager dbManager = Wolfia.dbManager;
        final EntityManager em = dbManager.getEntityManager();
        final GameStats g;
        try {
            g = em.find(GameStats.class, id);
            if (g != null) {
                //force load all the lazy things
                g.getActions().size();
                g.getStartingTeams().size();
                for (final TeamStats t : g.getStartingTeams()) {
                    t.getPlayers().size();
                }
            }
        } finally {
            em.close();
        }
        return g;
    }

    public static List<GameStats> loadFullStats() {
        final DbManager dbManager = Wolfia.dbManager;
        final EntityManager em = dbManager.getEntityManager();

        final List<GameStats> queryResult;
        try {
            queryResult = em.createQuery("SELECT g FROM stats_game g", GameStats.class).getResultList();
            //force load all the lazy things
            for (final GameStats g : queryResult) {
                g.getActions().size();
                g.getStartingTeams().size();
                for (final TeamStats t : g.getStartingTeams()) {
                    t.getPlayers().size();
                }
            }
        } finally {
            em.close();
        }
        return queryResult;
    }

    //may return null
    private static <O> O getObject(final long id, final Class<O> clazz) {
        final DbManager dbManager = Wolfia.dbManager;
        final EntityManager em = dbManager.getEntityManager();
        O object;
        try {
            object = em.find(clazz, id);
        } catch (final PersistenceException e) {
            log.error("Error while trying to find entity of class {} from DB for id {}", clazz.getName(), id, e);
            throw new RuntimeException(e);
        } finally {
            em.close();
        }

        return object;
    }

    private static <E extends IEntity> E newInstance(final long id, final Class<E> clazz) {
        try {
            final E entity = clazz.newInstance();
            entity.setId(id);
            return entity;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Could not create an entity of class " + clazz.getName(), e);
        }
    }

    public static List<SetupEntity> loadSetups() {
        final DbManager dbManager = Wolfia.dbManager;

        final EntityManager em = dbManager.getEntityManager();
        final List<SetupEntity> queryResult;
        try {
            queryResult = em.createQuery("SELECT s FROM SetupEntity s", SetupEntity.class).getResultList();
        } finally {
            em.close();
        }
        return queryResult;
    }


    //########## deletion

    public static void deleteEntity(final IEntity entity) {
        deleteEntity(entity.getId(), entity.getClass());
    }

    public static void deleteEntity(final long id, final Class<? extends IEntity> clazz) {
        final DbManager dbManager = Wolfia.dbManager;
        final EntityManager em = dbManager.getEntityManager();
        try {
            final IEntity entity = em.find(clazz, id);

            if (entity != null) {
                em.getTransaction().begin();
                em.remove(entity);
                em.getTransaction().commit();
            }
        } finally {
            em.close();
        }
    }
}
