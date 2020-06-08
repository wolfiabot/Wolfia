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

package space.npstr.wolfia.domain.oauth2;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import space.npstr.prometheus_extensions.OkHttpEventCounter;
import space.npstr.wolfia.config.properties.OAuth2Config;
import space.npstr.wolfia.db.type.OAuth2Scope;
import space.npstr.wolfia.webapi.OAuth2Endpoint;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Handle http request stuff related to oauth2
 */
@Component
public class OAuth2Requester {

    public static final String SCOPE_DELIMITER = " ";

    private static final Logger log = LoggerFactory.getLogger(OAuth2Requester.class);

    //see https://discord.com/developers/docs/topics/oauth2#shared-resources-oauth2-urls
    private static final String AUTHORIZATION_URL = "https://discord.com/api/oauth2/authorize";
    private static final String TOKEN_URL = "https://discord.com/api/oauth2/token";
    private static final String REVOCATION_URL = "https://discord.com/api/oauth2/token/revoke";

    private static final String DISCORD_API = "https://discord.com/api/v6";
    private static final String GET_USER_URL = DISCORD_API + "/users/@me";

    private final OAuth2Config oAuth2Config;
    private final OkHttpClient httpClient;

    public OAuth2Requester(OAuth2Config oAuth2Config, OkHttpClient.Builder httpClientBuilder) {
        this.oAuth2Config = oAuth2Config;
        this.httpClient = httpClientBuilder
                .eventListener(new OkHttpEventCounter("oauth2"))
                .build();
    }

    /**
     * @return the url that initializes oauth2 authorization with discord
     */
    public HttpUrl getAuthorizationUrl(String state) {
        return HttpUrl.get(AUTHORIZATION_URL).newBuilder()
                .addQueryParameter("client_id", this.oAuth2Config.getClientId())
                .addQueryParameter("redirect_uri", getRedirectUri())
                .addQueryParameter("state", state)
                .addQueryParameter("response_type", "code")
                .addQueryParameter("scope", getScopes(OAuth2Scope.IDENTIFY, OAuth2Scope.GUILD_JOIN))
                .build();
    }

    /**
     * See https://discord.com/developers/docs/topics/oauth2#authorization-code-grant-access-token-exchange-example
     */
    @CheckReturnValue
    public CompletionStage<AccessTokenResponse> fetchCodeResponse(String code) {
        RequestBody requestBody = new FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", getRedirectUri())
                .add("scope", getScopes(OAuth2Scope.IDENTIFY, OAuth2Scope.GUILD_JOIN))
                .build();

        String authorization = Credentials.basic(this.oAuth2Config.getClientId(), this.oAuth2Config.getClientSecret());
        Request request = new Request.Builder()
                .url(TOKEN_URL)
                .header("Authorization", authorization)
                .post(requestBody)
                .build();

        CompletableFuture<Response> oAuth2Callback = new CompletableFuture<>();
        this.httpClient.newCall(request)
                .enqueue(asCallback(oAuth2Callback));

        return oAuth2Callback.thenApply(this::toAccessTokenResponse);
    }

    /**
     * See https://discord.com/developers/docs/resources/user#get-current-user
     *
     * @return the id of the user who the passed in accessToken belongs to
     */
    @CheckReturnValue
    public CompletionStage<Long> identifyUser(String accessToken) {
        Request userInfoRequest = new Request.Builder()
                .url(GET_USER_URL)
                .header("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        CompletableFuture<Response> userInfoCallback = new CompletableFuture<>();
        this.httpClient.newCall(userInfoRequest)
                .enqueue(asCallback(userInfoCallback));

        return userInfoCallback
                .thenApply(this::extractUserId);
    }

    /**
     * See https://discord.com/developers/docs/topics/oauth2#authorization-code-grant-refresh-token-exchange-example
     *
     * @return a refreshed {@link OAuth2Data}
     */
    @CheckReturnValue
    public CompletionStage<OAuth2Data> refresh(OAuth2Data old) {
        RequestBody requestBody = new FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", old.refreshToken())
                .add("redirect_uri", getRedirectUri())
                .add("scope", getScopes(old.scopes())
                )
                .build();

        String authorization = Credentials.basic(this.oAuth2Config.getClientId(), this.oAuth2Config.getClientSecret());
        Request refreshRequest = new Request.Builder()
                .url(TOKEN_URL)
                .header("Authorization", authorization)
                .post(requestBody)
                .build();

        CompletableFuture<Response> refreshCallback = new CompletableFuture<>();
        this.httpClient.newCall(refreshRequest)
                .enqueue(asCallback(refreshCallback));

        return refreshCallback
                .thenApply(this::toAccessTokenResponse)
                .thenApply(accessTokenResponse -> new OAuth2Data(
                                old.userId(),
                                accessTokenResponse.accessToken(),
                                accessTokenResponse.expires(),
                                accessTokenResponse.refreshToken(),
                                accessTokenResponse.scopes()
                        )
                );
    }

    private AccessTokenResponse toAccessTokenResponse(Response response) {
        String body;
        try (response) {
            body = unwrapBody(response);
        }

        JSONObject json = new JSONObject(body);
        String accessToken = json.getString("access_token");
        String refreshToken = json.getString("refresh_token");
        int expiresInSeconds = json.getInt("expires_in");
        Instant expires = Instant.now().plusSeconds(expiresInSeconds);
        String scope = json.getString("scope");
        Set<OAuth2Scope> scopes = Arrays.stream(scope.split(SCOPE_DELIMITER))
                .map(s -> {
                    Optional<OAuth2Scope> parsed = OAuth2Scope.parse(s);
                    if (parsed.isEmpty()) {
                        log.warn("Unknown scope: {}", s);
                    }
                    return parsed;
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());

        return ImmutableAccessTokenResponse.builder()
                .accessToken(accessToken)
                .expires(expires)
                .refreshToken(refreshToken)
                .addAllScopes(scopes)
                .build();
    }

    private long extractUserId(Response response) {
        String body;
        try (response) {
            body = unwrapBody(response);
        }

        JSONObject json = new JSONObject(body);
        String id = json.getString("id");
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new DiscordRequestFailedException("Discord returned a userId that is not a long, body: " + body, e);
        }
    }

    private String getRedirectUri() {
        String result = this.oAuth2Config.getBaseRedirectUrl();
        if (!result.endsWith("/")) {
            result += "/";
        }
        result += OAuth2Endpoint.CODE_GRANT_PATH;
        return result;
    }

    private String getScopes(OAuth2Scope... scopes) {
        return getScopes(Set.of(scopes));
    }

    private String getScopes(Set<OAuth2Scope> scopes) {
        return scopes.stream()
                .map(OAuth2Scope::discordName)
                .collect(Collectors.joining(SCOPE_DELIMITER));
    }

    private String unwrapBody(Response response) {
        if (!response.isSuccessful() || response.body() == null) {
            response.close();
            throw new DiscordRequestFailedException("Failed to call ddoscord, status " + response.code());
        }

        String body;
        try {
            body = response.body().string();
        } catch (IOException e) {
            response.close();
            throw new DiscordRequestFailedException("Failed to get body from ddoscord response, status " + response.code(), e);
        }

        return body;
    }

    private Callback asCallback(CompletableFuture<Response> completableFuture) {
        return new Callback() {
            @Override
            public void onFailure(@Nonnull Call call, @Nonnull IOException e) {
                completableFuture.completeExceptionally(e);
            }

            @Override
            public void onResponse(@Nonnull Call call, @Nonnull Response response) {
                completableFuture.complete(response);
            }
        };
    }
}
