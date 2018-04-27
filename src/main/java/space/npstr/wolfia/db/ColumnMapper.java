/*
 * Copyright (C) 2017-2018 Dennis Neufeld
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

import org.hibernate.Session;

import javax.persistence.EntityManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by napster on 27.04.18.
 */
public class ColumnMapper {

    private ColumnMapper() {
    }


    //########## stuff below is copy pasta from https://brixomatic.wordpress.com/2016/07/14/returning-the-result-of-a-jpa-native-query-as-a-simple-map-or-pojo/


    public static List<Map<String, Object>> asListOfMaps(final List<Object[]> queryResultAsListOfObjectArrays, final Map<String, Integer> columnNameToIndexMap) {
        final Function<Object[], Map<String, Object>> rowValueArrayToColumnNameToValueMap = rowValueArray -> getColumNameToValueMapFromRowValueArray(rowValueArray, columnNameToIndexMap);
        return queryResultAsListOfObjectArrays.stream().map(rowValueArrayToColumnNameToValueMap).collect(Collectors.toList());
    }

    //remember to close the passed EntityManager outside of this
    public static Map<String, Integer> getColumnNameToIndexMap(final String queryString, final EntityManager em) throws
                                                                                                                 SQLException {

        final Session session = em.unwrap(Session.class); // ATTENTION! This is Hibernate-specific!
        final AtomicReference<ResultSetMetaData> msRef = new AtomicReference<>();
        session.doWork((c) -> {
            try (final PreparedStatement statement = create(c, queryString)) {
                // I'm not setting parameters here, because I just want to find out about the return values' column names
                msRef.set(statement.getMetaData());
            }
        });
        final ResultSetMetaData metaData = msRef.get();
        // LinkedHashmap preserves order of insertion:
        final Map<String, Integer> columnNameToColumnIndex = new LinkedHashMap<>();
        for (int t = 0; t < metaData.getColumnCount(); ++t) {
            // important, first index in the metadata is "1", the first index for the result array must be "0"
            columnNameToColumnIndex.put(metaData.getColumnName(t + 1), t);
        }
        return columnNameToColumnIndex;
    }

    private static Map<String, Object> getColumNameToValueMapFromRowValueArray(final Object[] rowValueArray, final Map<String, Integer> columnNameToIndexMap) {
        // stream().collect(toMap(keyFunct, valueFunct)...) will not accept "null" values, so we do it this way:
        final Map<String, Object> result = new LinkedHashMap<>();
        columnNameToIndexMap.forEach((key, value) -> result.put(key, rowValueArray[value]));
        return result;
    }

    private static PreparedStatement create(final Connection connection, final String queryStringWithNamedParameters)
            throws SQLException {
        final String parsedQuery = parse(queryStringWithNamedParameters);
        return connection.prepareStatement(parsedQuery);
    }

    private static String parse(final String query) {
        // I was originally using regular expressions, but they didn't work well for ignoring
        // parameter-like strings inside quotes.
        final int length = query.length();
        final StringBuilder parsedQuery = new StringBuilder(length);
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < length; ++i) {
            char c = query.charAt(i);
            if (inSingleQuote) {
                if (c == '\'') {
                    inSingleQuote = false;
                }
            } else if (inDoubleQuote) {
                if (c == '"') {
                    inDoubleQuote = false;
                }
            } else {
                if (c == '\'') {
                    inSingleQuote = true;
                } else if (c == '"') {
                    inDoubleQuote = true;
                } else if (c == ':' && i + 1 < length && Character.isJavaIdentifierStart(query.charAt(i + 1))) {
                    int j = i + 2;
                    while (j < length && Character.isJavaIdentifierPart(query.charAt(j))) {
                        ++j;
                    }
                    final String name = query.substring(i + 1, j);
                    c = '?'; // replace the parameter with a question mark
                    i += name.length(); // skip past the end of the parameter
                }
            }
            parsedQuery.append(c);
        }
        return parsedQuery.toString();
    }

}
