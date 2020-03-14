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

import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static space.npstr.wolfia.TestUtil.uniqueLong;

class RedisKeyParserTest {

    private final RedisKeyParser redisKeyParser = new RedisKeyParser();

    @Test
    void whenFromCorrectKey_returnOriginalUserId() {
        long userId = uniqueLong();
        String redisKey = this.redisKeyParser.toKey(userId);

        Optional<Long> parsed = this.redisKeyParser.fromKey(redisKey);

        assertThat(parsed).hasValue(userId);
    }

    @Test
    void whenFromGibberishKey_returnEmpty() {
        String gibberish = "foo";

        Optional<Long> parsed = this.redisKeyParser.fromKey(gibberish);

        assertThat(parsed).isEmpty();
    }

    @Test
    void whenFromNonNumericKey_returnEmpty() {
        String nonNumeric = "user:Rick:last_active";

        Optional<Long> parsed = this.redisKeyParser.fromKey(nonNumeric);

        assertThat(parsed).isEmpty();
    }

}
