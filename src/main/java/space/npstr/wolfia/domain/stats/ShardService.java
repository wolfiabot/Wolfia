/*
 * Copyright (C) 2016-2020 the original author or authors
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
package space.npstr.wolfia.domain.stats;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import org.springframework.stereotype.Service;


import javax.annotation.CheckReturnValue;

@Service
public class ShardService {

    private final ShardManager shardManager;

    public ShardService(ShardManager shardManager) {
        this.shardManager = shardManager;
    }
    @CheckReturnValue
    public JSONObject getShardStatus() {
        JSONObject result = new JSONObject();
        JSONArray arr = new JSONArray();

        List<JDA> sorted = new ArrayList<>(shardManager.getShards());
        sorted.sort(Comparator.comparingInt(o -> o.getShardInfo().getShardId()));

        for (JDA jda: sorted) {
            JSONObject shard = new JSONObject();

            shard.put("id", jda.getShardInfo().getShardId());
            shard.put("status", jda.getStatus());

            arr.add(shard);
        }

        result.put("shards", arr);

        return result;
    }
}