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

package space.npstr.wolfia.webapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import space.npstr.wolfia.common.Exceptions;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.domain.oauth2.AuthCommand;
import space.npstr.wolfia.domain.oauth2.AuthState;
import space.npstr.wolfia.domain.oauth2.AuthStateCache;
import space.npstr.wolfia.domain.oauth2.DiscordRequestFailedException;
import space.npstr.wolfia.domain.oauth2.OAuth2Service;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static space.npstr.wolfia.common.Exceptions.logIfFailed;

@RestController
@RequestMapping("/" + OAuth2Endpoint.CODE_GRANT_PATH)
public class OAuth2Endpoint extends BaseEndpoint {

    public static final String CODE_GRANT_PATH = "public/oauth2/discord";
    public static final String GENERIC_ERROR_RESPONSE = "Something was off with your request. Try authorizing again with "
            + WolfiaConfig.DEFAULT_PREFIX + AuthCommand.TRIGGER;
    public static final String WRONG_ACCOUNT_RESPONSE = "It looks like you are logged into a different account in your browser."
            + " than in your app. Log out in your browser, say " + WolfiaConfig.DEFAULT_PREFIX + AuthCommand.TRIGGER
            + " to start authication again, and then log in into the same account you are using to play Wolfia.";
    public static final String DISCORD_ISSUES = "Something went :boom: when talking to Discord. Try authorizing again with "
            + WolfiaConfig.DEFAULT_PREFIX + AuthCommand.TRIGGER + " or try again later.";

    private static final Logger log = LoggerFactory.getLogger(OAuth2Endpoint.class);

    private final OAuth2Service service;
    private final AuthStateCache stateCache;

    public OAuth2Endpoint(OAuth2Service service, AuthStateCache stateCache) {
        this.service = service;
        this.stateCache = stateCache;
    }

    @GetMapping
    public CompletionStage<ResponseEntity<String>> codeGrant(@RequestParam("code") String code,
                                                             @RequestParam(name = "state", required = false) @Nullable String state) {

        var authStateOpt = this.stateCache.getAuthState(state);
        if (authStateOpt.isEmpty()) {
            return CompletableFuture.completedStage(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(GENERIC_ERROR_RESPONSE));
        }

        AuthState authState = authStateOpt.get();

        return this.service.acceptCode(code)
                .thenApply(data -> {
                    if (data.userId() != authState.userId()) {
                        log.info("Flow initiated by user {} was finished by user {}", authState.userId(), data.userId());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(WRONG_ACCOUNT_RESPONSE);
                    }

                    String scopes = data.scopes().stream()
                            .map(Enum::name)
                            .collect(Collectors.joining(", "));
                    log.info("User {} authorized with scopes {}", data.userId(), scopes);

                    HttpHeaders headers = new HttpHeaders();
                    headers.setLocation(URI.create(authState.redirectUrl()));
                    return new ResponseEntity<>("", headers, HttpStatus.TEMPORARY_REDIRECT);
                })
                .whenComplete(logIfFailed())
                .exceptionally(t -> {
                    Throwable realCause = Exceptions.unwrap(t);
                    if (realCause instanceof DiscordRequestFailedException) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DISCORD_ISSUES);
                    }
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(GENERIC_ERROR_RESPONSE);
                });
    }

}
