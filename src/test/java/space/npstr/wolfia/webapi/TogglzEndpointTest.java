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

package space.npstr.wolfia.webapi;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import space.npstr.wolfia.ApplicationTest;
import space.npstr.wolfia.config.properties.WolfiaConfig;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * For some reason mockMvc returns 404s against the togglz endpoint, so we're using a different request mechanism
 * for tests that need to hit the endpoint
 */
public class TogglzEndpointTest extends ApplicationTest {

    private static final String TOGGLZ_PATH = "/api/togglz";

    @Autowired
    private OkHttpClient httpClient;

    @Autowired
    private WolfiaConfig config;

    @Test
    void whenGet_withoutAuthentication_returnUnauthorized() throws Exception {
        mockMvc.perform(get(TOGGLZ_PATH))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenGet_withRandomAuthentication_returnUnauthorized() throws Exception {
        mockMvc.perform(get(TOGGLZ_PATH)
                .with(httpBasic("foo", "bar")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenGet_withWebAdminAuthentication_returnOk() throws IOException {
        String credentials = Credentials.basic(config.getWebAdmin(), config.getWebPass());
        Request request = request()
                .header("Authorization", credentials)
                .build();

        Response response = httpClient.newCall(request).execute();

        assertThat(response.code()).isEqualTo(HttpStatus.OK.value());
    }

    private Request.Builder request() {
        return new Request.Builder()
                .get()
                .url("http://localhost:" + port + "/api/togglz");
    }
}
