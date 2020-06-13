/*
 * Copyright (C) 2016-2020 Dennis Neufeld
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

package space.npstr.wolfia.webapi;

import java.net.URI;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultMatcher;
import space.npstr.wolfia.App;
import space.npstr.wolfia.ApplicationTest;
import space.npstr.wolfia.DiscordApiConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static space.npstr.wolfia.TestUtil.uniqueLong;

class InviteEndpointTest extends ApplicationTest {

    @Test
    void whenGet_redirects() throws Exception {
        mockMvc.perform(get("/invite"))
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(isProperInviteUrl());
    }

    private ResultMatcher isProperInviteUrl() {
        return result -> {
            String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
            assertThat(location).isNotNull();
            URI uri = URI.create(location);
            assertThat(uri.getScheme()).isEqualTo("https");
            assertThat(uri.getHost()).isEqualTo("discord.com");

            HttpUrl httpUrl = HttpUrl.get(uri);
            assertThat(httpUrl).isNotNull();
            assertThat(httpUrl.pathSegments()).containsExactly("oauth2", "authorize");
            assertThat(httpUrl.queryParameter("client_id")).isEqualTo(Long.toString(DiscordApiConfig.SELF_ID));
            assertThat(httpUrl.queryParameter("scope")).isEqualTo("bot");
            assertThat(httpUrl.queryParameter("permissions")).isEqualTo("268787777");
            assertThat(httpUrl.queryParameter("response_type")).isEqualTo("code");
            assertThat(httpUrl.queryParameter("redirect_uri")).isEqualTo(App.WOLFIA_LOUNGE_INVITE);
        };
    }

    @Test
    void whenGetWithGuildId_redirectsWithGuildId() throws Exception {
        long guildId = uniqueLong();
        mockMvc.perform(get("/invite?guild_id={guildId}", guildId))
                .andExpect(hasGuildId(guildId));
    }

    private ResultMatcher hasGuildId(long guildId) {
        return result -> {
            String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
            assertThat(location).isNotNull();
            HttpUrl httpUrl = HttpUrl.get(location);
            assertThat(httpUrl).isNotNull();
            assertThat(httpUrl.queryParameter("guild_id")).isEqualTo(Long.toString(guildId));
        };
    }

}
