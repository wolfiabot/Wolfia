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

package space.npstr.wolfia.db;

import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.impl.SQLDataType;
import org.jooq.util.postgres.PostgresDSL;

/**
 * DSL for our own functions & stored procedures.
 * <p>
 * Note: JOOQ (due to JVM constraints) cannot extract the correct datatype from collections when calling
 * {@link org.jooq.impl.DSL#value(Object)}, so use arrays.
 */
public class ExtendedPostgresDSL extends PostgresDSL {

    /**
     * See V2 migration.
     */
    @SafeVarargs
    public static <T> Field<T[]> arrayAppendDistinct(Field<T[]> array, T... values) {
        return arrayAppendDistinct(array, value(values));
    }

    /**
     * See V2 migration.
     */
    public static <T> Field<T[]> arrayAppendDistinct(Field<T[]> array1, Field<T[]> array2) {
        return function("array_append_distinct", nullSafeDataTypeCopy(array1), array1, array2);
    }

    /**
     * See V2 migration.
     */
    @SafeVarargs
    public static <T> Field<T[]> arrayDiff(Field<T[]> array1, T... values) {
        return arrayDiff(array1, value(values));
    }

    /**
     * See V2 migration.
     */
    public static <T> Field<T[]> arrayDiff(Field<T[]> array1, Field<T[]> array2) {
        return function("array_diff", nullSafeDataTypeCopy(array1), array1, array2);
    }

    // Deprecated in jOOQ 3.15, so we copy it
    @SuppressWarnings("unchecked")
    private static <T> DataType<T> nullSafeDataTypeCopy(Field<T> field) {
        return (DataType<T>) (field == null ? SQLDataType.OTHER : field.getDataType());
    }

    private ExtendedPostgresDSL() {}
}
