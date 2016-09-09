package de.npstr.wolfia.utils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.lambdaworks.redis.api.sync.RedisCommands;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by npstr on 06.09.2016
 * <p>
 * K must be String
 */
public class DBWrapper {

    private static final Logger LOG = LogManager.getLogger();


    private String DB_PREFIX;
    private RedisCommands<String, String> redis;
    private Gson gson;

    public DBWrapper(String prefix, RedisCommands<String, String> redis, Gson gson) {
        this.DB_PREFIX = prefix;
        this.redis = redis;
        this.gson = gson;
    }

    private String set(String key, String value) {
        LOG.trace("db call: setting key [" + key + "] to [" + value + "]");
        return redis.set(key, value);
    }

    private String get(String key) {
        LOG.trace("db call: getting key [" + key + "]");
        return redis.get(key);
    }


    public <T> String set(String key, T object) {
        key = DB_PREFIX + key;
        String value = gson.toJson(object);
        return this.set(key, value);
    }

    public <T> T get(String key, Class<T> classOfT) {
        key = DB_PREFIX + key;
        String qvalue = this.get(key);
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

    public Long del(String key) {
        key = DB_PREFIX + key;
        LOG.trace("db call: deleting key [" + key + "]");
        return redis.del(key);
    }
}
