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

package space.npstr.wolfia.domain.setup.lastactive;

import net.dv8tion.jda.api.entities.User;
import org.springframework.stereotype.Service;
import space.npstr.wolfia.config.properties.WolfiaConfig;

import java.time.Duration;

@Service
public class ActivityService {

    private static final Duration DEFAULT_ACTIVITY_TIMEOUT = Duration.ofMinutes(20);

    private final LastActiveRepository repository;
    private final WolfiaConfig wolfiaConfig;

    public ActivityService(LastActiveRepository repository, WolfiaConfig wolfiaConfig) {
        this.repository = repository;
        this.wolfiaConfig = wolfiaConfig;
    }

    public void recordActivity(User user) {
        recordActivity(user.getIdLong());
    }

    public void recordActivity(long userId) {
        Duration activityTimeout = this.wolfiaConfig.isDebug()
                ? Duration.ofSeconds(30)
                : DEFAULT_ACTIVITY_TIMEOUT;

        this.repository.recordActivity(userId, activityTimeout)
                .toCompletableFuture().join();
    }

    public boolean wasActiveRecently(User user) {
        return wasActiveRecently(user.getIdLong());
    }

    public boolean wasActiveRecently(long userId) {
        return this.repository.wasActiveRecently(userId)
                .toCompletableFuture().join();
    }
}
