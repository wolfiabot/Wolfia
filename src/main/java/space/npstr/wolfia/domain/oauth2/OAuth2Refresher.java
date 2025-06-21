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

package space.npstr.wolfia.domain.oauth2;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import space.npstr.wolfia.game.tools.ExceptionLoggingExecutor;

@Component
public class OAuth2Refresher {

    private static final Logger log = LoggerFactory.getLogger(OAuth2Refresher.class);

    private static final Duration TWO_DAYS = Duration.ofDays(2);

    private final OAuth2Repository repository;
    private final OAuth2Requester oAuth2Requester;

    public OAuth2Refresher(ExceptionLoggingExecutor executor, OAuth2Repository repository,
                           OAuth2Requester oAuth2Requester) {
        this.repository = repository;
        this.oAuth2Requester = oAuth2Requester;

        executor.scheduleAtFixedRate(this::refresh, 1, 1, TimeUnit.HOURS);
    }

    private void refresh() {
        List<OAuth2Data> expiringSoon = this.repository.findAllExpiringIn(TWO_DAYS);
        log.debug("{} oauth data are expiring soon", expiringSoon.size());

        for (OAuth2Data old : expiringSoon) {
            try {
                this.repository.save(oAuth2Requester.refreshBlocking(old));
            } catch (Exception e) {
                log.warn("Failed to refresh token for user {}", old.userId(), e);
                // TODO DM user about it?
                if (old.expires().isBefore(Instant.now())) {
                    this.repository.delete(old.userId());
                }
            }
        }
    }
}
