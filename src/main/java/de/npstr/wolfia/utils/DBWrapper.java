package de.npstr.wolfia.utils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.lambdaworks.redis.api.sync.RedisCommands;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Created by npstr on 06.09.2016
 * <p>
 * K must be String
 */
public class DBWrapper {

    private static final Logger LOG = LogManager.getLogger();


    private final String DB_PREFIX;
    private final RedisCommands<String, String> redis;
    private final Gson gson;

    public DBWrapper(String prefix, RedisCommands<String, String> redis, Gson gson) {
        this.DB_PREFIX = prefix;
        this.redis = redis;
        this.gson = gson;
    }

    private String setStr(String key, String value) {
        LOG.trace("db call: setting key [" + key + "] to [" + value + "]");
        return redis.set(key, value);
    }

    private String getStr(String key) {
        LOG.trace("db call: getting key [" + key + "]");
        return redis.get(key);
    }

    public String set(String key, String value) {
        key = DB_PREFIX + key;
        return this.setStr(key, value);
    }

    public String get(String key) {
        key = DB_PREFIX + key;
        return this.getStr(key);
    }

    public <T> String set(String key, T object) {
        key = DB_PREFIX + key;
        String value = gson.toJson(object);
        return this.setStr(key, value);
    }

    /**
     * @param key
     * @param classOfT
     * @param <T>
     * @return the requested item or null
     */
    public <T> T get(String key, Class<T> classOfT) {
        key = DB_PREFIX + key;
        String qvalue = this.getStr(key);
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

    public long del(String key) {
        key = DB_PREFIX + key;
        LOG.trace("db call: deleting key [" + key + "]");
        return redis.del(key);
    }


    //hash related stuff

    public long hdel(String key) {
        key = DB_PREFIX + key;
        LOG.trace("db call: deleting entire hash [" + key + "]");
        return redis.hdel(key);
    }

    public boolean hset(String key, String field, String value) {
        key = DB_PREFIX + key;
        LOG.trace("db call: setting field [" + field + "] of hash [" + key + "] to [" + value + "]");
        return redis.hset(key, field, value);
    }

    public String hget(String key, String field) {
        key = DB_PREFIX + key;
        LOG.trace("db call: getting field [" + field + "] of hash [" + key + "]");
        return redis.hget(key, field);
    }

    public Map<String, String> hgetall(String key) {
        key = DB_PREFIX + key;
        LOG.trace("db call: getting entire hash [" + key + "]");
        return redis.hgetall(key);
    }
}
