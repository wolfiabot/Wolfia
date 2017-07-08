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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.db.entity.Hstore;
import space.npstr.wolfia.db.entity.PrivateGuild;
import space.npstr.wolfia.db.entity.SetupEntity;
import space.npstr.wolfia.db.entity.stats.GameStats;
import space.npstr.wolfia.db.entity.stats.TeamStats;
import space.npstr.wolfia.utils.DatabaseException;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    public static <O> O merge(final O object) throws DatabaseException {
        final DbManager dbManager = Wolfia.dbManager;
        final EntityManager em = dbManager.getEntityManager();
        O managedObject;
        try {
            em.getTransaction().begin();
            managedObject = em.merge(object);
            em.getTransaction().commit();
        } catch (final PersistenceException e) {
            log.error("Failed to merge object {}", object, e);
            throw new DatabaseException("Failed to merge object", e);
        } finally {
            em.close();
        }
        return managedObject;
    }

    public static void persist(final Object object) throws DatabaseException {
        final DbManager dbManager = Wolfia.dbManager;
        final EntityManager em = dbManager.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(object);
            em.getTransaction().commit();
        } catch (final PersistenceException e) {
            log.error("Failed to merge object {}", object, e);
            throw new DatabaseException("Failed to merge object", e);
        } finally {
            em.close();
        }
    }

    public static int executeJPQLQuery(final String queryString, final Map<String, Object> parameters) throws DatabaseException {
        final DbManager dbManager = Wolfia.dbManager;
        final EntityManager em = dbManager.getEntityManager();
        try {
            final Query query = em.createQuery(queryString);
            parameters.forEach(query::setParameter);
            em.getTransaction().begin();
            final int updatedOrDeleted = query.executeUpdate();
            em.getTransaction().commit();
            return updatedOrDeleted;
        } catch (final PersistenceException e) {
            log.error("Failed to execute JPQL query {}", queryString, e);
            throw new DatabaseException("Failed to execute JPQL query", e);
        } finally {
            em.close();
        }
    }

    //########## loading

    //never returns null; IEntity objects are required to have a default constructor that sets them up with sensible
    // defaults, as long as we make sure that the id is a natural one (for example derived from a snowflake from
    // discord, aka channels, guilds, users etc)
    public static <E extends IEntity> E getEntity(final long id, final Class<E> clazz) throws DatabaseException {
        E entity = getObject(id, clazz);
        //return a fresh object if we didn't find the one we were looking for
        if (entity == null) {
            entity = newInstance(id, clazz);
            entity = DbWrapper.merge(entity);
        }
        return entity;
    }

    public static GameStats loadSingleGameStats(final long id) throws DatabaseException {
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
        } catch (final PersistenceException e) {
            log.error("Failed to load single game stats for game #{}", id, e);
            throw new DatabaseException("Failed to load single game stats", e);
        } finally {
            em.close();
        }
        return g;
    }

    public static List<GameStats> loadFullStats() throws DatabaseException {
        final DbManager dbManager = Wolfia.dbManager;
        final EntityManager em = dbManager.getEntityManager();

        final List<GameStats> queryResult;
        try {
            queryResult = em.createQuery("FROM GameStats", GameStats.class).getResultList();
            //force load all the lazy things
            for (final GameStats g : queryResult) {
                g.getActions().size();
                g.getStartingTeams().size();
                for (final TeamStats t : g.getStartingTeams()) {
                    t.getPlayers().size();
                }
            }
        } catch (final PersistenceException e) {
            log.error("Failed to full game stats", e);
            throw new DatabaseException("Failed to full game stats", e);
        } finally {
            em.close();
        }
        return queryResult;
    }

    public static List<PrivateGuild> loadPrivateGuilds() throws DatabaseException {
        return loadAll("FROM PrivateGuild", PrivateGuild.class);
    }

    public static <E> List<E> loadAll(final String query, final Class<E> clazz) throws DatabaseException {
        final DbManager dbManager = Wolfia.dbManager;
        final EntityManager em = dbManager.getEntityManager();
        final List<E> queryResult = new ArrayList<>();
        try {
            queryResult.addAll(em.createQuery(query, clazz).getResultList());
        } catch (final PersistenceException e) {
            log.error("Failed to load all {}", query, e);
            throw new DatabaseException("Failed to load all", e);
        } finally {
            em.close();
        }
        return queryResult;
    }

    //may return null
    private static <O> O getObject(final long id, final Class<O> clazz) throws DatabaseException {
        final DbManager dbManager = Wolfia.dbManager;
        final EntityManager em = dbManager.getEntityManager();
        O object;
        try {
            object = em.find(clazz, id);
        } catch (final PersistenceException e) {
            log.error("Error while trying to find entity of class {} from DB for id {}", clazz.getName(), id, e);
            throw new DatabaseException("Error while trying to find entity", e);
        } finally {
            em.close();
        }

        return object;
    }

    private static <E extends IEntity> E newInstance(final long id, final Class<E> clazz) throws DatabaseException {
        try {
            final E entity = clazz.newInstance();
            entity.setId(id);
            return entity;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new DatabaseException("Could not create an entity of class " + clazz.getName(), e);
        }
    }

    public static List<SetupEntity> loadSetups() throws DatabaseException {
        final DbManager dbManager = Wolfia.dbManager;

        final EntityManager em = dbManager.getEntityManager();
        final List<SetupEntity> queryResult;
        try {
            queryResult = em.createQuery("SELECT s FROM SetupEntity s", SetupEntity.class).getResultList();
        } catch (final PersistenceException e) {
            log.error("Failed to load setups", e);
            throw new DatabaseException("Failed to load setups", e);
        } finally {
            em.close();
        }
        return queryResult;
    }

    /**
     * @param queryString the raw JPQL query string
     * @param parameters  parameters to be set on the query
     * @param resultClass expected class of the results of the query
     * @param offset      set to -1 or lower for no offset
     * @param limit       set to -1 or lower for no limit
     */
    //limited and offset results
    public static <T> List<T> selectJPQLQuery(final String queryString, final Map<String, Object> parameters,
                                              final Class<T> resultClass, final int offset, final int limit) throws DatabaseException {
        final DbManager dbManager = Wolfia.dbManager;
        final EntityManager em = dbManager.getEntityManager();
        try {
            final TypedQuery<T> q = em.createQuery(queryString, resultClass);
            parameters.forEach(q::setParameter);
            if (offset > -1) q.setFirstResult(offset);
            if (limit > -1) q.setMaxResults(limit);
            return q.getResultList();
        } catch (final PersistenceException e) {
            log.error("Failed to select JPQL query {}", queryString, e);
            throw new DatabaseException("Failed to select JPQL query", e);
        } finally {
            em.close();
        }
    }

    //limited results
    public static <T> List<T> selectJPQLQuery(final String queryString, final Map<String, Object> parameters,
                                              final Class<T> resultClass, final int limit) throws DatabaseException {
        return selectJPQLQuery(queryString, parameters, resultClass, -1, limit);
    }

    public static <T> List<T> selectJPQLQuery(final String queryString, final Class<T> resultClass,
                                              final int limit) throws DatabaseException {
        return selectJPQLQuery(queryString, Collections.emptyMap(), resultClass, -1, limit);
    }

    //all results
    public static <T> List<T> selectJPQLQuery(final String queryString, final Map<String, Object> parameters,
                                              final Class<T> resultClass) throws DatabaseException {
        return selectJPQLQuery(queryString, parameters, resultClass, -1);
    }

    public static <T> List<T> selectJPQLQuery(final String queryString, final Class<T> resultClass) throws DatabaseException {
        return selectJPQLQuery(queryString, Collections.emptyMap(), resultClass, -1);
    }

    //########## deletion

    public static void deleteEntity(final IEntity entity) throws DatabaseException {
        deleteEntity(entity.getId(), entity.getClass());
    }

    public static void deleteEntity(final long id, final Class<? extends IEntity> clazz) throws DatabaseException {
        final DbManager dbManager = Wolfia.dbManager;
        final EntityManager em = dbManager.getEntityManager();
        try {
            final IEntity entity = em.find(clazz, id);

            if (entity != null) {
                em.getTransaction().begin();
                em.remove(entity);
                em.getTransaction().commit();
            }
        } catch (final PersistenceException e) {
            log.error("Failed to delete entity id {} of class {}", id, clazz.getSimpleName(), e);
            throw new DatabaseException("Failed to delete entity", e);
        } finally {
            em.close();
        }
    }


    //########## hstore stuff

    public static Hstore getHstore(final String name) {
        final DbManager dbManager = Wolfia.dbManager;
        final EntityManager em = dbManager.getEntityManager();
        try {
            Hstore hstore = em.find(Hstore.class, name);

            //create a fresh one
            if (hstore == null) {
                hstore = new Hstore(name);
            }

            return hstore;
        } catch (final PersistenceException e) {
            log.error("Failed to load hstore of name {}", name, e);
            throw new DatabaseException("Failed to load hstore", e);
        } finally {
            em.close();
        }
    }
}
