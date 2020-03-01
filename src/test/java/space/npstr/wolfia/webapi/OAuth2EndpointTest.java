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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import space.npstr.wolfia.ApplicationTest;
import space.npstr.wolfia.db.type.OAuth2Scope;
import space.npstr.wolfia.domain.oauth2.AccessTokenResponse;
import space.npstr.wolfia.domain.oauth2.AuthState;
import space.npstr.wolfia.domain.oauth2.AuthStateCache;
import space.npstr.wolfia.domain.oauth2.DiscordRequestFailedException;
import space.npstr.wolfia.domain.oauth2.ImmutableAccessTokenResponse;
import space.npstr.wolfia.domain.oauth2.ImmutableAuthState;

import java.util.EnumSet;

import static java.time.OffsetDateTime.now;
import static java.util.concurrent.CompletableFuture.completedStage;
import static java.util.concurrent.CompletableFuture.failedStage;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static space.npstr.wolfia.TestUtil.uniqueLong;

class OAuth2EndpointTest extends ApplicationTest {

    private static final String CODE_GRANT_PATH = "/" + OAuth2Endpoint.CODE_GRANT_PATH;
    private static final String REDIRECT_URL = "https://example.org";
    private static final String ACCESS_TOKEN = "42";
    private static final String CODE = "69";

    @Autowired
    private AuthStateCache stateCache;

    @BeforeEach
    void setup() {
        AccessTokenResponse accessTokenResponse = accessTokenResponse();
        when(oAuth2Requester.fetchCodeResponse(eq(CODE)))
                .thenReturn(completedStage(accessTokenResponse));
    }

    //ensure that this endpoint is accessible
    @Test
    void whenGet_andSuccessful_redirect() throws Exception {
        long userId = uniqueLong();

        when(oAuth2Requester.identifyUser(eq(ACCESS_TOKEN)))
                .thenReturn(completedStage(userId));

        AuthState authState = authState(userId);
        String stateParam = stateCache.generateStateParam(authState);

        MvcResult asyncResult = mockMvc.perform(get(CODE_GRANT_PATH)
                .param("code", CODE)
                .param("state", stateParam))
                .andExpect(request().asyncStarted())
                .andReturn();
        asyncResult.getAsyncResult();

        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isTemporaryRedirect())
                .andExpect(redirectedUrl(REDIRECT_URL));
    }

    @Test
    void whenGet_noCodeParam_return400() throws Exception {
        AuthState authState = authState(uniqueLong());
        String stateParam = stateCache.generateStateParam(authState);
        mockMvc.perform(get(CODE_GRANT_PATH)
                .param("state", stateParam))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    void whenGet_noStateParam_return400() throws Exception {
        MvcResult asyncResult = mockMvc.perform(get(CODE_GRANT_PATH)
                .param("code", CODE))
                .andExpect(request().asyncStarted())
                .andReturn();
        asyncResult.getAsyncResult();

        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString(OAuth2Endpoint.GENERIC_ERROR_RESPONSE)));
    }

    @Test
    void whenGet_noCachedState_return400() throws Exception {
        long userId = uniqueLong();

        when(oAuth2Requester.identifyUser(eq(ACCESS_TOKEN)))
                .thenReturn(completedStage(userId));

        MvcResult asyncResult = mockMvc.perform(get(CODE_GRANT_PATH)
                .param("code", CODE)
                .param("state", "42"))
                .andExpect(request().asyncStarted())
                .andReturn();
        asyncResult.getAsyncResult();

        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString(OAuth2Endpoint.GENERIC_ERROR_RESPONSE)));
    }

    @Test
    void whenGet_differentUserId_return400() throws Exception {
        long userIdA = uniqueLong();
        long userIdB = uniqueLong();

        when(oAuth2Requester.identifyUser(eq(ACCESS_TOKEN)))
                .thenReturn(completedStage(userIdA));

        AuthState authState = authState(userIdB);
        String stateParam = stateCache.generateStateParam(authState);

        MvcResult asyncResult = mockMvc.perform(get(CODE_GRANT_PATH)
                .param("code", CODE)
                .param("state", stateParam))
                .andExpect(request().asyncStarted())
                .andReturn();
        asyncResult.getAsyncResult();

        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString(OAuth2Endpoint.WRONG_ACCOUNT_RESPONSE)));
    }

    @Test
    void whenGet_identificationFails_return400() throws Exception {
        when(oAuth2Requester.identifyUser(eq(ACCESS_TOKEN)))
                .thenReturn(failedStage(new DiscordRequestFailedException("lol nope")));

        AuthState authState = authState(uniqueLong());
        String stateParam = stateCache.generateStateParam(authState);

        MvcResult asyncResult = mockMvc.perform(get(CODE_GRANT_PATH)
                .param("code", CODE)
                .param("state", stateParam))
                .andExpect(request().asyncStarted())
                .andReturn();
        asyncResult.getAsyncResult();

        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString(OAuth2Endpoint.DISCORD_ISSUES)));
    }

    @Test
    void whenGet_randomException_return500() throws Exception {
        when(oAuth2Requester.identifyUser(eq(ACCESS_TOKEN)))
                .thenReturn(failedStage(new RuntimeException("lol nope")));

        AuthState authState = authState(uniqueLong());
        String stateParam = stateCache.generateStateParam(authState);

        MvcResult asyncResult = mockMvc.perform(get(CODE_GRANT_PATH)
                .param("code", CODE)
                .param("state", stateParam))
                .andExpect(request().asyncStarted())
                .andReturn();
        asyncResult.getAsyncResult();

        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString(OAuth2Endpoint.GENERIC_ERROR_RESPONSE)));
    }


    private AccessTokenResponse accessTokenResponse() {
        return ImmutableAccessTokenResponse.builder()
                .accessToken(ACCESS_TOKEN)
                .expires(now().plusMonths(1).toInstant())
                .refreshToken(ACCESS_TOKEN)
                .addAllScopes(EnumSet.allOf(OAuth2Scope.class))
                .build();
    }

    private AuthState authState(long userId) {
        return ImmutableAuthState.builder()
                .userId(userId)
                .redirectUrl(REDIRECT_URL)
                .build();
    }
}
