/*
 * Copyright (C) 2016-2019 Dennis Neufeld
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

import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.cache.SnowflakeCacheView;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import space.npstr.wolfia.config.ShardManagerFactory;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Profile("test")
@Configuration
public class DiscordApiConfig {

    // the shardManagerFactory is required to ensure the same dependencies between beans exist
    // in the test application context as in the real application context
    @SuppressWarnings("unused")
    @Bean
    public ShardManager shardManager(ShardManagerFactory shardManagerFactory) {
        return createMockShardManager();
    }

    private ShardManager createMockShardManager() {
        ShardManager shardManager = mock(ShardManager.class);

        RestAction restAction = mock(RestAction.class);
        doReturn(restAction).when(shardManager).retrieveApplicationInfo();

        SnowflakeCacheView guildCache = mock(SnowflakeCacheView.class);
        doReturn(guildCache).when(shardManager).getGuildCache();

        return shardManager;
    }

}
