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

package space.npstr.wolfia.domain.privacy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class PrivacyService {

    private static final Logger log = LoggerFactory.getLogger(PrivacyService.class);

    private final ApplicationEventPublisher eventPublisher;
    private final PrivacyRepository privacyRepository;

    public PrivacyService(ApplicationEventPublisher eventPublisher, PrivacyRepository privacyRepository) {
        this.eventPublisher = eventPublisher;
        this.privacyRepository = privacyRepository;
    }

    public boolean isDataProcessingEnabled(long userId) {
        return this.privacyRepository.findOne(userId)
                .toCompletableFuture().join()
                .map(Privacy::isProcessData)
                .orElse(true);
    }

    public void dataDelete(long userId) {
        this.privacyRepository.setProcessData(userId, false)
                .toCompletableFuture().join();
        try {
            this.eventPublisher.publishEvent(ImmutablePersonalDataDelete.builder()
                    .userId(userId)
                    .build()
            );
        } catch (Exception e) {
            log.warn("Something went wrong when publishing data deletion for user {}", userId, e);
        }
    }

    public void request() {
        throw new UnsupportedOperationException(); //TODO implement
    }
}
