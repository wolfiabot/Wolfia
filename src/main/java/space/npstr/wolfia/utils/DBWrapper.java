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

package space.npstr.wolfia.utils;

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

    private static final Logger LOG = LoggerFactory.getLogger(DBWrapper.class);

    private static final long EXPIRE = 1 * 60 * 60 * 24 * 60; //60 days ~ 2 months

    private final String DB_PREFIX;
    private final RedisCommands<String, String> redis;
    private final Gson gson;

    public DBWrapper(String prefix, RedisCommands<String, String> redis, Gson gson) {
        this.DB_PREFIX = prefix;
        this.redis = redis;
        this.gson = gson;
    }

    //setting stuff
    private String setStr(String key, String value, boolean persist) {
        LOG.trace("db call: setting key [" + key + "] to [" + value + "]");
        String result = redis.set(key, value);
        if (!persist) redis.expire(key, EXPIRE);
        return result;
    }


    //set strings
    public String set(String key, String value, boolean persist) {
        key = DB_PREFIX + key;
        return this.setStr(key, value, persist);
    }

    public String set(String key, String value) {
        return this.set(key, value, false);
    }

    //set objects
    public <T> String set(String key, T object, boolean persist) {
        key = DB_PREFIX + key;
        String value = gson.toJson(object);
        return this.setStr(key, value, persist);
    }

    public <T> String set(String key, T object) {
        return this.set(key, object, false);
    }

    //getting stuff
    private String getStr(String key, boolean persist) {
        LOG.trace("db call: getting key [" + key + "]");
        if (!persist) redis.expire(key, EXPIRE);
        return redis.get(key);
    }

    //get strings
    public String get(String key, boolean persist) {
        key = DB_PREFIX + key;
        return this.getStr(key, persist);
    }

    public String get(String key) {
        return this.get(key, false);
    }

    //get objects

    /**
     * @return the requested item or null
     */
    public <T> T get(String key, Class<T> classOfT, boolean persist) {
        key = DB_PREFIX + key;
        String qvalue = this.getStr(key, persist);
        if (qvalue == null) {
            LOG.info("nothing in db behind key [" + key + "]");
            return null;
        }
        try {
            return gson.fromJson(qvalue, classOfT);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            LOG.error("value [" + qvalue + "] behind key [" + key + "] could not be converted to object of class " + classOfT);
            return null;
        }
    }

    /**
     * @return the requested item or null
     */
    public <T> T get(String key, Class<T> classOfT) {
        return this.get(key, classOfT, false);
    }


    public long del(String key) {
        key = DB_PREFIX + key;
        LOG.trace("db call: deleting key [" + key + "]");
        return redis.del(key);
    }


    //hash related stuff
    public boolean hset(String key, String field, String value, boolean persist) {
        key = DB_PREFIX + key;
        LOG.trace("db call: setting field [" + field + "] of hash [" + key + "] to [" + value + "]");
        boolean result = redis.hset(key, field, value);
        if (!persist) redis.expire(key, EXPIRE);
        return result;
    }

    public boolean hset(String key, String field, String value) {
        return this.hset(key, field, value, false);
    }


    public String hget(String key, String field, boolean persist) {
        key = DB_PREFIX + key;
        LOG.trace("db call: getting field [" + field + "] of hash [" + key + "]");
        if (!persist) redis.expire(key, EXPIRE);
        return redis.hget(key, field);
    }

    public String hget(String key, String field) {
        return this.hget(key, field, false);
    }


    public Map<String, String> hgetall(String key, boolean persist) {
        key = DB_PREFIX + key;
        LOG.trace("db call: getting entire hash [" + key + "]");
        if (!persist) redis.expire(key, EXPIRE);
        return redis.hgetall(key);
    }

    public Map<String, String> hgetall(String key) {
        return this.hgetall(key, false);
    }


    public long hdel(String key) {
        key = DB_PREFIX + key;
        LOG.trace("db call: deleting entire hash [" + key + "]");
        return redis.hdel(key);
    }
}
