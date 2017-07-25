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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by napster on 23.07.17.
 * <p>
 * APIs of various bot listing sites are found in here
 */
public class Listings {

    private static final Logger log = LoggerFactory.getLogger(Listings.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");


    private static volatile String lastBotsDiscordPwPayload = "";
    private static volatile Future botsDiscordPwTask;


    public static synchronized void postToBotsDiscordPw(final JDA jda) {
        if (botsDiscordPwTask != null && !botsDiscordPwTask.isCancelled() && !botsDiscordPwTask.isDone()) {
            log.info("Skipping posting stats to bots.discord.pw since there is a task to do that running already.");
            return;
        }

        postToBotsDiscordPw(jda, 0);
    }

    //https://bots.discord.pw
    //api docs: https://bots.discord.pw/api
    //according to their discord: post these on guild join, guild leave, and ready events
    //which is bonkers given guild joins and leaves are wonky when discord is having issues
    private static void postToBotsDiscordPw(final JDA jda, final int attempt) {
        if (Config.C.isDebug) {
            log.info("Skipping posting stats to bots.discord.pw due to running in debug mode");
            return;
        }

        final String payload = new JSONObject().put("server_count", jda.getGuilds().size()).toString();
        if (payload.equals(lastBotsDiscordPwPayload)) {
            log.info("Skipping sending stats to bots.discord.pw since the payload has not changed");
            return;
        }

        final int att = attempt + 1;
        final RequestBody body = RequestBody.create(JSON, payload);
        final Request req = new Request.Builder()
                .url(String.format("https://bots.discord.pw/api/bots/%s/stats", jda.getSelfUser().getIdLong()))
                .addHeader("Authorization", Config.C.botsDiscordPwToken)
                .post(body)
                .build();
        try {
            final Response response = Wolfia.httpClient.newCall(req).execute();
            if (response.isSuccessful()) {
                log.info("Attempt {} successfully posted bot stats to bots.discord.pw, code {}", att, response.code());
                lastBotsDiscordPwPayload = payload;
            } else {
                log.warn("Attempt {} failed to post stats to bots.discord.pw: code {}, body:\n{}", att,
                        response.code(), response.body() != null ? response.body().string() : "null");
                botsDiscordPwTask = reschedule(() -> postToBotsDiscordPw(jda, att), att);
            }
        } catch (final IOException e) {
            log.warn("Attempt {} failed to post stats to bots.discord.pw", att, e);
            botsDiscordPwTask = reschedule(() -> postToBotsDiscordPw(jda, att), att);
        }
    }

    //increase delay with growing attempts to avoid overloading the listing servers
    private static Future reschedule(final Runnable task, final int attempt) {
        return Wolfia.schedule(task, 10 * attempt, TimeUnit.SECONDS);
    }

    private static volatile String lastDiscordbotsOrgPayload = "";
    private static volatile Future discordbotsOrgTask;

    public static synchronized void postToDiscordbotsOrg(final JDA jda) {
        if (discordbotsOrgTask != null && !discordbotsOrgTask.isCancelled() && !discordbotsOrgTask.isDone()) {
            log.info("Skipping posting stats to discordbots.org since there is a task to do that running already.");
            return;
        }
        postToDiscordbotsOrg(jda, 0);
    }

    //https://discordbots.org/
    //api docs: https://discordbots.org/api/docs
    private static void postToDiscordbotsOrg(final JDA jda, final int attempt) {
        if (Config.C.isDebug) {
            log.info("Skipping posting stats to discordbots.org due to running in debug mode");
            return;
        }

        final String payload = new JSONObject().put("server_count", jda.getGuilds().size()).toString();
        if (payload.equals(lastDiscordbotsOrgPayload)) {
            log.info("Skipping sending stats to discordbots.org since the payload has not changed");
            return;
        }

        final int att = attempt + 1;
        final RequestBody body = RequestBody.create(JSON, payload);
        final Request req = new Request.Builder()
                .url(String.format("https://discordbots.org/api/bots/%s/stats", jda.getSelfUser().getIdLong()))
                .addHeader("Authorization", Config.C.discordbotsOrgToken)
                .post(body)
                .build();
        try {
            final Response response = Wolfia.httpClient.newCall(req).execute();
            if (response.isSuccessful()) {
                log.info("Attempt {} successfully posted bot stats to discordbots.org, code {}", att, response.code());
                lastDiscordbotsOrgPayload = payload;
            } else {
                log.warn("Attempt {} failed to post stats to discordbots.org: code {}, body:\n{}", att,
                        response.code(), response.body() != null ? response.body().string() : "null");
                discordbotsOrgTask = reschedule(() -> postToDiscordbotsOrg(jda, att), att);
            }
        } catch (final IOException e) {
            log.warn("Attempt {} failed to post stats to discordbots.org", attempt, e);
            discordbotsOrgTask = reschedule(() -> postToDiscordbotsOrg(jda, att), att);
        }
    }
}
