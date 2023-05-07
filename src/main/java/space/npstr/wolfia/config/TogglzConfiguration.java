/*
 * Copyright (C) 2016-2023 the original author or authors
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

package space.npstr.wolfia.config;

import java.time.Clock;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.togglz.core.logging.LoggingStateRepository;
import org.togglz.core.repository.StateRepository;
import org.togglz.core.repository.composite.CompositeStateRepository;
import org.togglz.core.user.UserProvider;
import org.togglz.spring.security.SpringSecurityUserProvider;
import space.npstr.wolfia.db.Database;
import space.npstr.wolfia.system.togglz.ExceptionTolerantCachingStateRepo;
import space.npstr.wolfia.system.togglz.PostgresJdbcStateRepo;
import space.npstr.wolfia.system.togglz.StatusLoggingStateRepo;
import space.npstr.wolfia.webapi.Authorization;

@Configuration
public class TogglzConfiguration {

    @Bean
    public StateRepository stateRepository(Database database, Clock clock, StatusLoggingStateRepo statusLoggingStateRepo) {
        var jdbcStateRepository = new PostgresJdbcStateRepo(database.getConnection().getDataSource());
        Duration ttl = Duration.ofMinutes(1);
        Duration stalePeriod = Duration.ofMinutes(1);
        var cachingJdbcStateRepo = new ExceptionTolerantCachingStateRepo(jdbcStateRepository,
                ttl, clock, stalePeriod);
        var loggingStateRepository = new LoggingStateRepository(cachingJdbcStateRepo);
        var compositeStateRepository = new CompositeStateRepository(statusLoggingStateRepo, loggingStateRepository);
        compositeStateRepository.setSetterSelection(CompositeStateRepository.SetterSelection.ALL);
        return compositeStateRepository;
    }

    @Bean
    public UserProvider userProvider() {
        return new SpringSecurityUserProvider(Authorization.INSTANCE.getOWNER().getAuthority());
    }
}
