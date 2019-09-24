/*
 * Copyright (C) 2017-2019 Dennis Neufeld
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

import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.requests.RestAction;
import net.dv8tion.jda.core.utils.cache.SnowflakeCacheView;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Profile("test")
@Configuration
public class DiscordApiConfig {

    @Bean
    public ShardManager shardManager() {
        ShardManager shardManager = mock(ShardManager.class);

        RestAction restAction = mock(RestAction.class);
        doReturn(restAction).when(shardManager).getApplicationInfo();

        SnowflakeCacheView guildCache = mock(SnowflakeCacheView.class);
        doReturn(guildCache).when(shardManager).getGuildCache();

        return shardManager;
    }

}