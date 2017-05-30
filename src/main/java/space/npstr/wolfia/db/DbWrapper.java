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

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import java.util.List;

/**
 * Created by napster on 30.05.17.
 * <p>
 * This class is all about saving/loading/deleting entities
 */
public class DbWrapper {

    private static final Logger log = LoggerFactory.getLogger(DbWrapper.class);


    //########## saving

    private static void merge(final IEntity entity) {
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

    //########## loading

    private static <E extends IEntity> E getEntity(final long id, final Class<E> clazz) {
        final DbManager dbManager = Wolfia.dbManager;

        final EntityManager em = dbManager.getEntityManager();
        E entity;
        try {
            entity = em.find(clazz, id);
        } catch (final PersistenceException e) {
            log.error("Error while trying to find entity of class {} from DB for id {}", clazz.getName(), id, e);
            throw new RuntimeException(e);
        } finally {
            em.close();
        }
        //return a fresh object if we didn't found the one we were looking for
        if (entity == null) entity = newInstance(id, clazz);
        return entity;
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
