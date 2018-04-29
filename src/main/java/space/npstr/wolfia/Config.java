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


import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import space.npstr.wolfia.utils.GitRepoState;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;

/**
 * /**
 * Created by npstr on 18.11.2016
 * <p>
 * Contains the config values loaded from disk
 */
public class Config {

    public static final String PREFIX = "w.";

    private static final Logger log = LoggerFactory.getLogger(Config.class);

    //avoid a (in this case unnecessary) gettocalypse by making all the values public final
    public static final Config C;

    static {
        Config c;
        try {
            c = new Config();
        } catch (final IOException e) {
            c = null;
            log.error("Could not load config files!" + e);
        }
        C = c;
    }

    public final boolean isDebug;

    public final String discordToken;
    public final String jdbcUrl;

    //listing site tokens
    public final String botsDiscordPwToken;
    public final String discordbotsOrgToken;
    public final String carbonitexKey;

    //logging and error handling
    public final long logChannelId;
    public final String errorLogWebHook;
    public final String sentryDsn;


    @SuppressWarnings(value = "unchecked")
    public Config() throws IOException {

        final File configFile = new File("wolfia.yaml");
        final Yaml yaml = new Yaml();
        try (final Reader reader = new InputStreamReader(new FileInputStream(configFile), "UTF-8")) {
            final Map<String, Object> config = yaml.load(reader);
            //change nulls to empty strings
            config.keySet().forEach((String key) -> config.putIfAbsent(key, ""));

            //where are we running?
            this.isDebug = (boolean) config.getOrDefault("isDebug", false);

            final Map<String, String> values;
            if (this.isDebug) {
                values = (Map) config.get("debug");
            } else {
                values = (Map) config.get("prod");
            }

            this.discordToken = values.getOrDefault("discordToken", "");
            this.jdbcUrl = values.getOrDefault("jdbcUrl", "");

            //listing site tokens
            this.botsDiscordPwToken = values.getOrDefault("botsDiscordPwToken", "");
            this.discordbotsOrgToken = values.getOrDefault("discordbotsOrgToken", "");
            this.carbonitexKey = values.getOrDefault("carbonitexKey", "");

            this.errorLogWebHook = values.getOrDefault("errorLogWebHook", "");
            this.logChannelId = Long.parseLong(values.getOrDefault("logChannelId", "0"));
            this.sentryDsn = values.getOrDefault("sentryDsn", "");
            if (this.sentryDsn != null && !this.sentryDsn.isEmpty()) {
                Sentry.init(this.sentryDsn).setRelease(GitRepoState.getGitRepositoryState().commitId);
            }
        }
    }
}
