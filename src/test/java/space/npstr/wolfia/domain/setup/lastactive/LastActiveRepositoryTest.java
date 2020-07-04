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

package space.npstr.wolfia.domain.setup.lastactive;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import space.npstr.wolfia.ApplicationTest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static space.npstr.wolfia.TestUtil.sleep;
import static space.npstr.wolfia.TestUtil.uniqueLong;

class LastActiveRepositoryTest extends ApplicationTest {

    @Autowired
    private LastActiveRepository repository;

    @Test
    void whenNotActive_returnFalse() {
        boolean active = repository.wasActiveRecently(uniqueLong()).toCompletableFuture().join();

        assertThat(active).isFalse();
    }

    @Test
    void whenActive_returnTrue() {
        long userId = uniqueLong();
        repository.recordActivity(userId, Duration.ofHours(1)).toCompletableFuture().join();

        boolean active = repository.wasActiveRecently(userId).toCompletableFuture().join();

        assertThat(active).isTrue();
    }

    @Test
    void whenActive_returnFalseAfterTimeout() {
        long userId = uniqueLong();
        repository.recordActivity(userId, Duration.ofMillis(50)).toCompletableFuture().join();
        sleep(Duration.ofMillis(60));

        boolean active = repository.wasActiveRecently(userId).toCompletableFuture().join();

        assertThat(active).isFalse();
    }
}
