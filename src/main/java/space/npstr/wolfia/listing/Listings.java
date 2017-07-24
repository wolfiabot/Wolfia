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

package space.npstr.wolfia.listing;

import net.dv8tion.jda.core.JDA;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;

import java.io.IOException;

/**
 * Created by napster on 23.07.17.
 * <p>
 * APIs of various bot listing sites are found in here
 */
public class Listings {

    private static final Logger log = LoggerFactory.getLogger(Listings.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");


    private static volatile String lastBotsDiscordPwPayload = "";

    //https://bots.discord.pw
    //api docs: https://bots.discord.pw/api
    //according to their discord: post these on guild join, guild leave, and ready events
    //which is bonkers given guild joins and leaves are wonky when discord is having issues
    public static synchronized void postToBotsDiscordPw(final JDA jda) {
        if (Config.C.isDebug) {
            log.info("Skipping posting stats to bots.discord.pw due to running in debug mode");
            return;
        }

        final String payload = new JSONObject().put("server_count", jda.getGuilds().size()).toString();
        if (payload.equals(lastBotsDiscordPwPayload)) {
            log.info("Skipping sending stats to bots.discord.pw since the payload has not changed");
            return;
        }

        final RequestBody body = RequestBody.create(JSON, payload);
        final Request req = new Request.Builder()
                .url(String.format("https://bots.discord.pw/api/bots/%s/stats", jda.getSelfUser().getIdLong()))
                .addHeader("Authorization", Config.C.botsDiscordPwToken)
                .post(body)
                .build();
        try {
            final Response response = Wolfia.httpClient.newCall(req).execute();
            if (response.isSuccessful()) {
                log.info("Successfully posted bot stats to bots.discord.pw, code {}", response.code());
                lastBotsDiscordPwPayload = payload;
            } else {
                log.error("Failed to post stats to bots.discord.pw: code {}, body:\n{}", response.code(),
                        response.body() != null ? response.body().string() : "null");
            }
        } catch (final IOException e) {
            log.error("Failed to post stats to bots.discord.pw", e);
        }
    }

    private static volatile String lastDiscordbotsOrgPayload = "";

    //https://discordbots.org/
    //api docs: https://discordbots.org/api/docs
    public static synchronized void postToDiscordbotsOrg(final JDA jda) {
        if (Config.C.isDebug) {
            log.info("Skipping posting stats to bots.discord.pw due to running in debug mode");
            return;
        }
        final String payload = new JSONObject().put("server_count", jda.getGuilds().size()).toString();
        if (payload.equals(lastDiscordbotsOrgPayload)) {
            log.info("Skipping sending stats to discordbots.org since the payload has not changed");
            return;
        }

        final RequestBody body = RequestBody.create(JSON, payload);
        final Request req = new Request.Builder()
                .url(String.format("https://discordbots.org/api/bots/%s/stats", jda.getSelfUser().getIdLong()))
                .addHeader("Authorization", Config.C.discordbotsOrgToken)
                .post(body)
                .build();
        try {
            final Response response = Wolfia.httpClient.newCall(req).execute();
            if (response.isSuccessful()) {
                log.info("Successfully posted bot stats to discordbots.org, code {}", response.code());
                lastDiscordbotsOrgPayload = payload;
            } else {
                log.error("Failed to post stats to discordbots.org: code {}, body:\n{}", response.code(),
                        response.body() != null ? response.body().string() : "null");
            }
        } catch (final IOException e) {
            log.error("Failed to post stats to discordbots.org", e);
        }
    }
}
