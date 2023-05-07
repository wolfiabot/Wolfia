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

package space.npstr.wolfia.domain.setup.lastactive;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class RedisKeyParser {

    private static final String PREFIX = "user";
    private static final String SUFFIX = "last_active";
    private static final String USER_KEY_FORMAT = PREFIX + ":%s:" + SUFFIX;
    // https://regex101.com/r/UXjQlB/1
    private static final Pattern USER_KEY_PATTERN = Pattern.compile(PREFIX + ":(\\d+):" + SUFFIX);

    public String toKey(long userId) {
        return String.format(USER_KEY_FORMAT, userId);
    }

    public Optional<Long> fromKey(String key) {
        Matcher matcher = USER_KEY_PATTERN.matcher(key);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        try {
            long userId = Long.parseLong(matcher.group(1));
            return Optional.of(userId);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
