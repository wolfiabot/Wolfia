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

package space.npstr.wolfia.db;

import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import space.npstr.wolfia.db.gen.tables.records.HstorexRecord;

import javax.annotation.CheckReturnValue;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import static org.jooq.impl.DSL.val;
import static space.npstr.wolfia.db.gen.Tables.HSTOREX;

@Repository
public class HstoreRepository {

    private final AsyncDbWrapper wrapper;

    public HstoreRepository(AsyncDbWrapper asyncDbWrapper) {
        this.wrapper = asyncDbWrapper;
    }

    /**
     * @return the default value if either the hstore or the key inside the hstore doesnt exist.
     */
    @CheckReturnValue
    public CompletionStage<String> get(String name, String key, String defaultValue) {
        return this.wrapper.jooq(dsl -> dsl
                .select(HSTOREX.HSTOREX_)
                .from(HSTOREX)
                .where(HSTOREX.NAME.eq(name))
                .fetchOptional(HSTOREX.HSTOREX_)
                .map(map -> map.getOrDefault(key, defaultValue))
                .orElse(defaultValue)
        );
    }

    @CheckReturnValue
    public CompletionStage<HstorexRecord> set(String name, String key, String value) {
        HashMap<String, String> toAppend = new HashMap<>(Map.of(key, value));
        return set(name, toAppend);
    }

    @CheckReturnValue
    public CompletionStage<HstorexRecord> set(String name, Map<String, String> toAppend) {
        HashMap<String, String> map = new HashMap<>(toAppend);
        return this.wrapper.jooq(dsl -> dsl.transactionResult(config -> DSL.using(config)
                .insertInto(HSTOREX)
                .columns(HSTOREX.NAME, HSTOREX.HSTOREX_)
                .values(name, map)
                .onDuplicateKeyUpdate()
                .set(HSTOREX.HSTOREX_, concat(HSTOREX.HSTOREX_, val(map)))
                .returning()
                .fetchOne()
        ));
    }


    //source: https://stackoverflow.com/questions/27864026/update-hstore-fields-using-jooq
    private static Field<HashMap<String, String>> concat(
            Field<HashMap<String, String>> f1,
            Field<HashMap<String, String>> f2) {
        return DSL.field("{0} || {1}", f1.getDataType(), f1, f2);
    }
}
