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
import space.npstr.wolfia.Config;

import javax.annotation.Nonnull;

/**
 * Created by napster on 06.10.17.
 */
public class DiscordBotsOrg extends Listing {

    //https://discordbots.org/
    //api docs: https://discordbots.org/api/docs
    public DiscordBotsOrg(@Nonnull final OkHttpClient httpClient) {
        super("discordbots.org", httpClient);
    }

    @Nonnull
    @Override
    protected String createPayload(@Nonnull final JDA jda) {
        return new JSONObject()
                .put("server_count", jda.getGuildCache().size())
                .put("shard_id", jda.getShardInfo().getShardId())
                .put("shard_count", jda.getShardInfo().getShardTotal())
                .toString();
    }

    @Nonnull
    @Override
    protected Request.Builder createRequest(final long botId, @Nonnull final String payload) {
        final RequestBody body = RequestBody.create(JSON, payload);
        return new Request.Builder()
                .addHeader("user-agent", "Wolfia DiscordBot (" + App.GITHUB_LINK + ", " + App.VERSION + ")")
                .url(String.format("https://discordbots.org/api/bots/%s/stats", botId))
                .addHeader("Authorization", Config.C.discordbotsOrgToken)
                .post(body);
    }
}
