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

package space.npstr.wolfia.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Inspired by https://dzone.com/articles/java-8-automatic-memoization
 */
public class Memoizer<U> {

    private final Map<String, U> cache = new ConcurrentHashMap<>();

    private Memoizer() {}

    private Supplier<U> doMemoize(Supplier<U> supplier) {
        return () -> cache.computeIfAbsent("memo", __ -> supplier.get());
    }

    public static <U> Supplier<U> memoize(Supplier<U> supplier) {
        return new Memoizer<U>().doMemoize(supplier);
    }
}
