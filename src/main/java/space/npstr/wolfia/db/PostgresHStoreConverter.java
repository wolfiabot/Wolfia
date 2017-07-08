/*
 * Copyright (C) 2017 Dennis Neufeld
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

import org.postgresql.util.HStoreConverter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Map;

/**
 * Created by napster on 07.07.17.
 * <p>
 * Glue between JPA and postgres's hstore extension
 * When using this, you will likely need to set "stringtype" to "unspecified" in your JDBC url/parameters
 * You also need to enable the postgres hstore extension with
 * <p>
 * CREATE EXTENSION hstore;
 * <p>
 * for each database you want to use it in.
 */
@Converter(autoApply = true)
public class PostgresHStoreConverter implements AttributeConverter<Map<String, String>, String> {

    @Override
    public String convertToDatabaseColumn(final Map<String, String> attribute) {
        return HStoreConverter.toString(attribute);
    }

    @Override
    public Map<String, String> convertToEntityAttribute(final String dbData) {
        return HStoreConverter.fromString(dbData);
    }
}
