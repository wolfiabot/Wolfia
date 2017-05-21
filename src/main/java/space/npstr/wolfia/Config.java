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

package space.npstr.wolfia;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

/**
 * /**
 * Created by npstr on 18.11.2016
 * <p>
 * Contains the config values loaded from disk
 */
public class Config {

    //TODO sort these out
    public static final String BOT_ROLE_NAME = "Turbot";
    public static final String POPCORN_PLAYER_ROLE_NAME = "Popcorn Player"; //these role names shouldn't be used on the server cause this bot will overwrite them
    //TODO: use this variable as the default prefix in every class that uses a prefix
    public static final String PREFIX = "w.";

    public static final String NAPSTER_ID = "166604053629894657";


    private static final Logger log = LoggerFactory.getLogger(Config.class);

    //avoid a (in this case unnecessary) gettocalypse by making all the values public final
    public static final Config C;

    static {
        Config c;
        try {
            c = new Config();
        } catch (IOException e) {
            c = null;
            log.error("Could not load config files!" + e);
        }
        C = c;
    }

    //config
    public final boolean isDebug;

    //sneaky sneaky
    public final String discordToken;
    public final String redisAuth;
    public final String errorLogWebHook;

    @SuppressWarnings("unchecked")
    public Config() throws IOException {

        File sneakyFile = new File("sneaky.yaml");
        File configFile = new File("config.yaml");

        Yaml yaml = new Yaml();

        Map<String, Object> config = (Map<String, Object>) yaml.load(new FileReader(configFile));
        Map<String, Object> sneaky = (Map<String, Object>) yaml.load(new FileReader(sneakyFile));
        //change nulls to empty strings
        config.keySet().forEach((String key) -> config.putIfAbsent(key, ""));
        sneaky.keySet().forEach((String key) -> sneaky.putIfAbsent(key, ""));

        //config stuff
        isDebug = (boolean) config.getOrDefault("debug", false);

        //sneaky stuff
        Map<String, String> tokens = (Map) sneaky.get("discordToken");
        if (tokens != null)
            if (isDebug)
                discordToken = tokens.getOrDefault("debug", "");
            else
                discordToken = tokens.getOrDefault("prod", "");
        else
            discordToken = "";

        Map<String, String> redis = (Map) sneaky.get("redisAuth");
        if (redis != null)
            if (isDebug)
                redisAuth = redis.getOrDefault("debug", "");
            else
                redisAuth = redis.getOrDefault("prod", "");
        else
            redisAuth = "";

        errorLogWebHook = (String) sneaky.getOrDefault("errorLogWebHook", "");
    }
}
