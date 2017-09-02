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

package space.npstr.wolfia.db.entity;

import space.npstr.wolfia.db.DbWrapper;
import space.npstr.wolfia.db.PostgresHStoreConverter;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by napster on 06.07.17.
 * <p>
 * Basic HStore table
 * <p>
 * JPA-only dependant = not Hibernate or other vendors dependant
 * <p>
 * The x makes it sound awesome and also prevents a name/type collision in postgres
 */
@Entity
@Table(name = "hstorex")
public class Hstore {

    public static final String DEFAULT_HSTORE_NAME = "wolfia";

    //you are responsible for using unique names when you want to access unique hstores
    @Id
    @Column(name = "name")
    public String name;

    @Column(name = "hstorex", columnDefinition = "hstore")
    @Convert(converter = PostgresHStoreConverter.class)
    public final Map<String, String> hstore = new HashMap<>();

    //for jpa
    Hstore() {

    }


    public Hstore(final String name) {
        this.name = name;
    }

    /**
     * Convenience method to commit several changes at once
     * Use after calling set()
     * Example:
     * Hstore.loadAndSet("a", "a").set("b", "b").set("c", "c").save();
     *
     * @return the merged object
     */
    public Hstore save() {
        return DbWrapper.merge(this);
    }

    /**
     * @return itself for chaining calls
     */
    public Hstore set(final String key, final String value) {
        this.hstore.put(key, value);
        return this;
    }

    /**
     * @return the requested value
     */
    public String get(final String key, final String defaultValue) {
        return this.hstore.getOrDefault(key, defaultValue);
    }

    /**
     * @return the requested value or null if it doesnt exist
     */
    public String get(final String key) {
        return this.hstore.getOrDefault(key, null);
    }


    // ########## static convenience stuff below

    /**
     * @return load a value from an hstore object
     */
    public static String loadAndGet(final String name, final String key, final String defaultValue) {
        return DbWrapper.getHstore(name).hstore.getOrDefault(key, defaultValue);
    }

    /**
     * @return loads a value from the default hstore
     */
    public static String loadAndGet(final String key, final String defaultValue) {
        return loadAndGet(DEFAULT_HSTORE_NAME, key, defaultValue);
    }

    /**
     * @return the requested Hstore object
     */
    public static Hstore load(final String name) {
        return DbWrapper.getHstore(name);
    }

    /**
     * @return the default Hstore object
     */
    public static Hstore load() {
        return load(DEFAULT_HSTORE_NAME);
    }

    /**
     * Load an Hstore object
     *
     * @return the object for chaining calls; dont forget to merge() the changes
     */
    public static Hstore loadAndSet(final String name, final String key, final String value) {
        return load(name).set(key, value);
    }

    /**
     * Uses the default hstore
     *
     * @return the object for chaining calls; dont forget to merge() the changes
     */
    public static Hstore loadAndSet(final String key, final String value) {
        return loadAndSet(DEFAULT_HSTORE_NAME, key, value);
    }

}
