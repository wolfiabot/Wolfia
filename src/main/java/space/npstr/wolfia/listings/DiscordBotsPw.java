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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONObject;
import space.npstr.wolfia.App;
import space.npstr.wolfia.Launcher;

import javax.annotation.Nonnull;

/**
 * Created by napster on 06.10.17.
 */
public class DiscordBotsPw extends Listing {

    //https://bots.discord.pw
    //api docs: https://bots.discord.pw/api
    public DiscordBotsPw(@Nonnull final OkHttpClient httpClient) {
        super("bots.discord.pw", httpClient);
    }

    @Nonnull
    @Override
    protected String createPayload(@Nonnull final JDA jda) {
        return new JSONObject()
                .put("shard_id", jda.getShardInfo().getShardId())
                .put("shard_count", jda.getShardInfo().getShardTotal())
                .put("server_count", jda.getGuildCache().size())
                .toString();
    }

    @Nonnull
    @Override
    protected Request.Builder createRequest(final long botId, @Nonnull final String payload) {
        final RequestBody body = RequestBody.create(payload, JSON);
        return new Request.Builder()
                .addHeader("user-agent", "Wolfia DiscordBot (" + App.GITHUB_LINK + ", " + App.VERSION + ")")
                .url(String.format("https://bots.discord.pw/api/bots/%s/stats", botId))
                .addHeader("Authorization", Launcher.getBotContext().getListingsConfig().getBotsPwToken())
                .post(body);
    }

    @Override
    protected boolean isConfigured() {
        final String botsPwToken = Launcher.getBotContext().getListingsConfig().getBotsPwToken();
        return botsPwToken != null && !botsPwToken.isEmpty();
    }
}
