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

package space.npstr.wolfia.listings;

import net.dv8tion.jda.core.JDA;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import space.npstr.wolfia.Launcher;
import space.npstr.wolfia.Wolfia;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * Created by napster on 06.10.17.
 * <p>
 * Template for various bot listing sites
 */
public abstract class Listing {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Listing.class);

    protected static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    protected final String name;
    private final OkHttpClient httpClient;

    private String lastPayload;

    public Listing(@Nonnull final String name, @Nonnull final OkHttpClient httpClient) {
        this.name = name;
        this.httpClient = httpClient;
    }

    @Nonnull
    protected abstract String createPayload(@Nonnull JDA jda);

    @Nonnull
    protected abstract Request.Builder createRequest(long botId, @Nonnull String payload);

    //return false if there is no token configured, or whatever is needed to post to the site
    protected abstract boolean isConfigured();

    //retries with growing delay until it is successful
    public void postStats(@Nonnull final JDA jda) throws InterruptedException {
        if (!isConfigured()) {
            log.debug("Skipping posting stats to {} due to not being configured", this.name);
            return;
        }

        if (Launcher.getBotContext().getWolfiaConfig().isDebug()) {
            log.info("Skipping posting stats to {} due to running in debug mode", this.name);
            return;
        }

        if (this instanceof Carbonitex && !Wolfia.allShardsUp()) {
            log.info("Skipping posting stats to Carbonitex since not all shards are up");
            return;
        }

        final String payload = createPayload(jda);

        if (payload.equals(this.lastPayload)) {
            log.info("Skipping sending stats to {} since the payload has not changed", this.name);
            return;
        }

        final Request req = createRequest(jda.getSelfUser().getIdLong(), payload).build();

        int attempt = 0;
        boolean success = false;
        while (!success) {
            attempt++;
            try (final Response response = this.httpClient.newCall(req).execute()) {
                if (response.isSuccessful()) {
                    log.info("Successfully posted bot stats to {} on attempt {}, code {}", this.name, attempt, response.code());
                    this.lastPayload = payload;
                    success = true;
                } else {
                    //noinspection ConstantConditions
                    log.info("Failed to post stats to {} on attempt {}: code {}, body:\n{}",
                            this.name, attempt, response.code(), response.body().string());
                }
            } catch (final IOException e) {
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
    public boolean equals(final Object obj) {
        return obj instanceof Listing && this.name.equals(((Listing) obj).name);
    }
}
