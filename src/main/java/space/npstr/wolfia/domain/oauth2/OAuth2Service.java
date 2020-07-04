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

package space.npstr.wolfia.domain.oauth2;

import org.springframework.stereotype.Service;
import space.npstr.wolfia.db.type.OAuth2Scope;

import javax.annotation.CheckReturnValue;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static java.time.Instant.now;

@Service
public class OAuth2Service {

    private final OAuth2Repository repository;
    private final OAuth2Requester oAuth2Requester;

    public OAuth2Service(OAuth2Repository repository, OAuth2Requester oAuth2Requester) {
        this.repository = repository;
        this.oAuth2Requester = oAuth2Requester;
    }

    /**
     * @return the oauth2 access token for the requested user and scope, if such a token exists in our database,
     * empty otherwise
     * Note: the returned token may be invalid if the user revoked it
     */
    public Optional<String> getAccessTokenForScope(long userId, OAuth2Scope scope) {
        return this.repository.findOne(userId).toCompletableFuture().join()
                .filter(data -> now().isBefore(data.expires()))
                .filter((data -> data.scopes().contains(scope)))
                .map(OAuth2Data::accessToken);
    }

    /**
     * This completes the OAuth2 flow by fetching and saving the {@link OAuth2Data} of a user who authorized us.
     *
     * @param code code that we receive from Discord once a user visits our OAuth2 authorization url and authorizes us.
     */
    @CheckReturnValue
    public CompletionStage<OAuth2Data> acceptCode(String code) {
        return this.oAuth2Requester.fetchCodeResponse(code)
                .thenCompose(codeResponse -> this.oAuth2Requester.identifyUser(codeResponse.accessToken())
                        .thenApply(userId -> new OAuth2Data(userId,
                                codeResponse.accessToken(), codeResponse.expires(),
                                codeResponse.refreshToken(), codeResponse.scopes()
                        ))
                )
                .thenCompose(this.repository::save);
    }
}
