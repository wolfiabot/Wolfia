/*
 * Copyright (C) 2016-2025 the original author or authors
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

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.sharding.ShardManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONObject;
import space.npstr.wolfia.App;
import space.npstr.wolfia.config.properties.ListingsConfig;
import space.npstr.wolfia.config.properties.WolfiaConfig;

public class Carbonitex extends Listing {

    private final ShardManager shardManager;

    //https://www.carbonitex.net/
    //api docs: https://www.carbonitex.net/discord/data/botdata.php?key=MAH_KEY
    public Carbonitex(OkHttpClient httpClient, WolfiaConfig wolfiaConfig, ListingsConfig listingsConfig, ShardManager shardManager) {
        super("carbonitex.net", httpClient, wolfiaConfig, listingsConfig);
        this.shardManager = shardManager;
    }

    @Override
    protected String createPayload(JDA jda) {
        return new JSONObject()
                .put("key", listingsConfig.getCarbonitexKey())
                .put("servercount", shardManager.getGuildCache().size())
                .toString();
    }

    @Override
    protected Request.Builder createRequest(long botId, String payload) {
        RequestBody body = RequestBody.create(payload, JSON);
        return new Request.Builder()
                .addHeader("user-agent", "Wolfia DiscordBot (" + App.GITHUB_LINK + ", " + App.VERSION + ")")
                .url("https://www.carbonitex.net/discord/data/botdata.php")
                .post(body);
    }

    @Override
    protected boolean isConfigured() {
        String carbonitexKey = listingsConfig.getCarbonitexKey();
        return carbonitexKey != null && !carbonitexKey.isEmpty();
    }
}
