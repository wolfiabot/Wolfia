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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.lambdaworks.redis.api.sync.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by npstr on 06.09.2016
 * <p>
 * unless specified not to, keys will expire after 60 days
 */
public class DBWrapper {

    private static final Logger log = LoggerFactory.getLogger(DBWrapper.class);

    private static final long EXPIRE = 1 * 60 * 60 * 24 * 60; //60 days ~ 2 months

    private final String DB_PREFIX;
    private final RedisCommands<String, String> redis;
    private final Gson gson;

    public DBWrapper(final String prefix, final RedisCommands<String, String> redis, final Gson gson) {
        this.DB_PREFIX = prefix;
        this.redis = redis;
        this.gson = gson;
    }

    //setting stuff
    private String setStr(final String key, final String value, final boolean persist) {
        log.trace("db call: setting key [" + key + "] to [" + value + "]");
        final String result = this.redis.set(key, value);
        if (!persist) this.redis.expire(key, EXPIRE);
        return result;
    }


    //set strings
    public String set(String key, final String value, final boolean persist) {
        key = this.DB_PREFIX + key;
        return this.setStr(key, value, persist);
    }

    public String set(final String key, final String value) {
        return this.set(key, value, false);
    }

    //set objects
    public <T> String set(String key, final T object, final boolean persist) {
        key = this.DB_PREFIX + key;
        final String value = this.gson.toJson(object);
        return this.setStr(key, value, persist);
    }

    public <T> String set(final String key, final T object) {
        return this.set(key, object, false);
    }

    //getting stuff
    private String getStr(final String key, final boolean persist) {
        log.trace("db call: getting key [" + key + "]");
        if (!persist) this.redis.expire(key, EXPIRE);
        return this.redis.get(key);
    }

    //get strings
    public String get(String key, final boolean persist) {
        key = this.DB_PREFIX + key;
        return this.getStr(key, persist);
    }

    public String get(final String key) {
        return this.get(key, false);
    }

    //get objects

    /**
     * @return the requested item or null
     */
    public <T> T get(String key, final Class<T> classOfT, final boolean persist) {
        key = this.DB_PREFIX + key;
        final String qvalue = this.getStr(key, persist);
        if (qvalue == null) {
            log.info("nothing in db behind key [" + key + "]");
            return null;
        }
        try {
            return this.gson.fromJson(qvalue, classOfT);
        } catch (final JsonSyntaxException e) {
            log.error("value [{}] behind key [{}] could not be converted to object of class {}", qvalue, key, classOfT, e);
            return null;
        }
    }

    /**
     * @return the requested item or null
     */
    public <T> T get(final String key, final Class<T> classOfT) {
        return this.get(key, classOfT, false);
    }


    public long del(String key) {
        key = this.DB_PREFIX + key;
        log.trace("db call: deleting key [" + key + "]");
        return this.redis.del(key);
    }


    //hash related stuff
    public boolean hset(String key, final String field, final String value, final boolean persist) {
        key = this.DB_PREFIX + key;
        log.trace("db call: setting field [" + field + "] of hash [" + key + "] to [" + value + "]");
        final boolean result = this.redis.hset(key, field, value);
        if (!persist) this.redis.expire(key, EXPIRE);
        return result;
    }

    public boolean hset(final String key, final String field, final String value) {
        return this.hset(key, field, value, false);
    }


    public String hget(String key, final String field, final boolean persist) {
        key = this.DB_PREFIX + key;
        log.trace("db call: getting field [" + field + "] of hash [" + key + "]");
        if (!persist) this.redis.expire(key, EXPIRE);
        return this.redis.hget(key, field);
    }

    public String hget(final String key, final String field) {
        return this.hget(key, field, false);
    }


    public Map<String, String> hgetall(String key, final boolean persist) {
        key = this.DB_PREFIX + key;
        log.trace("db call: getting entire hash [" + key + "]");
        if (!persist) this.redis.expire(key, EXPIRE);
        return this.redis.hgetall(key);
    }

    public Map<String, String> hgetall(final String key) {
        return this.hgetall(key, false);
    }


    public long hdel(String key) {
        key = this.DB_PREFIX + key;
        log.trace("db call: deleting entire hash [" + key + "]");
        return this.redis.hdel(key);
    }
}
