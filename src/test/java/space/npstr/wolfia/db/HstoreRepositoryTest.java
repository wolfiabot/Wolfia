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

package space.npstr.wolfia.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import space.npstr.wolfia.ApplicationTest;

import static org.assertj.core.api.Assertions.assertThat;

class HstoreRepositoryTest extends ApplicationTest {

    @Autowired
    private HstoreRepository repository;

    @Test
    void givenValuesHaveBeenSet_whenGet_valueShouldBeEqual() {
        String name = "a random hstore";
        String keyA = "keyA";
        String valueA = "valueA";
        String keyB = "keyB";
        String valueB = "valueB";

        this.repository.set(name, keyA, valueA).toCompletableFuture().join();
        this.repository.set(name, keyB, valueB).toCompletableFuture().join();

        String fetchedA = this.repository.get(name, keyA, "failedA").toCompletableFuture().join();
        String fetchedB = this.repository.get(name, keyB, "failedB").toCompletableFuture().join();

        assertThat(fetchedA).isEqualTo(valueA);
        assertThat(fetchedB).isEqualTo(valueB);
    }

}
