/*
 * Copyright (C) 2016-2023 the original author or authors
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

package space.npstr.wolfia.listings;

import java.io.IOException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.sharding.ShardManager;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import space.npstr.wolfia.config.properties.ListingsConfig;
import space.npstr.wolfia.config.properties.WolfiaConfig;

import static java.util.Objects.requireNonNull;

/**
 * Template for various bot listing sites
 */
public abstract class Listing {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Listing.class);

    protected static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    protected final String name;
    protected final OkHttpClient httpClient;
    protected final WolfiaConfig wolfiaConfig;
    protected final ListingsConfig listingsConfig;

    private String lastPayload;

    public Listing(String name, OkHttpClient httpClient, WolfiaConfig wolfiaConfig, ListingsConfig listingsConfig) {
        this.name = name;
        this.httpClient = httpClient;
        this.wolfiaConfig = wolfiaConfig;
        this.listingsConfig = listingsConfig;
    }

    protected abstract String createPayload(JDA jda);

    protected abstract Request.Builder createRequest(long botId, String payload);

    //return false if there is no token configured, or whatever is needed to post to the site
    protected abstract boolean isConfigured();

    //retries with growing delay until it is successful
    public void postStats(JDA jda) throws InterruptedException {
        if (!isConfigured()) {
            log.debug("Skipping posting stats to {} due to not being configured", this.name);
            return;
        }

        if (this.wolfiaConfig.isDebug()) {
            log.info("Skipping posting stats to {} due to running in debug mode", this.name);
            return;
        }

        if (this instanceof Carbonitex && !allShardsUp(requireNonNull(jda.getShardManager()))) {
            log.info("Skipping posting stats to Carbonitex since not all shards are up");
            return;
        }

        String payload = createPayload(jda);

        if (payload.equals(this.lastPayload)) {
            log.info("Skipping sending stats to {} since the payload has not changed", this.name);
            return;
        }

        Request req = createRequest(jda.getSelfUser().getIdLong(), payload).build();

        int attempt = 0;
        boolean success = false;
        while (!success) {
            attempt++;
            try (Response response = this.httpClient.newCall(req).execute()) {
                if (response.isSuccessful()) {
                    log.info("Successfully posted bot stats to {} on attempt {}, code {}", this.name, attempt, response.code());
                    this.lastPayload = payload;
                    success = true;
                } else {
                    //noinspection ConstantConditions
                    String body = response.body().string();
                    log.info("Failed to post stats to {} on attempt {}: code {}, body:\n{}",
                            this.name, attempt, response.code(), body);
                }
            } catch (IOException e) {
                log.info("Failed to post stats to {} on attempt {}", this.name, attempt, e);
            }

            if (!success) {
                //increase delay with growing attempts to avoid overloading the listing servers
                Thread.sleep(attempt * 10000L); //10 sec
            }

            if (attempt == 10 || attempt == 100) { // no need to spam these
                log.warn("Attempt {} to post stats to {} unsuccessful. See logs for details.", attempt, this.name);
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Listing && this.name.equals(((Listing) obj).name);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    private boolean allShardsUp(ShardManager shardManager) {
        if (shardManager.getShards().size() < shardManager.getShardsTotal()) {
            return false;
        }
        for (JDA jda : shardManager.getShards()) {
            if (jda.getStatus() != JDA.Status.CONNECTED) {
                return false;
            }
        }
        return true;
    }
}
