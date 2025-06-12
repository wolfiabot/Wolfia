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

package space.npstr.wolfia.db;

import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Objects;
import org.jooq.Binding;
import org.jooq.BindingGetResultSetContext;
import org.jooq.BindingGetSQLInputContext;
import org.jooq.BindingGetStatementContext;
import org.jooq.BindingRegisterContext;
import org.jooq.BindingSQLContext;
import org.jooq.BindingSetSQLOutputContext;
import org.jooq.BindingSetStatementContext;
import org.jooq.Converter;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.postgresql.util.HStoreConverter;

/**
 * We're binding <T> = Object (unknown JDBC type), and <U> = Map<String, String> (user type)
 */
public class PostgresHstoreBinding implements Binding<Object, HashMap<String, String>> {

    @Override
    public Converter<Object, HashMap<String, String>> converter() {
        return new Converter<>() {

            @Override
            public HashMap<String, String> from(Object databaseObject) {
                return databaseObject == null ? null : new HashMap<>(HStoreConverter.fromString("" + databaseObject));
            }

            @Override
            public String to(HashMap<String, String> userObject) {
                return userObject == null ? null : HStoreConverter.toString(userObject);
            }

            @Override
            public Class<Object> fromType() {
                return Object.class;
            }

            @SuppressWarnings("unchecked")
            @Override
            public Class<HashMap<String, String>> toType() {
                return (Class<HashMap<String, String>>) (Class<?>) HashMap.class;
            }
        };
    }

    @Override
    public void sql(BindingSQLContext<HashMap<String, String>> ctx) {
        if (ctx.render().paramType() == ParamType.INLINED) {
            ctx.render().visit(DSL.inline(ctx.convert(converter()).value())).sql("::hstore");
        } else {
            ctx.render().sql("?::hstore");
        }
    }

    @Override
    public void register(BindingRegisterContext<HashMap<String, String>> ctx) throws SQLException {
        ctx.statement().registerOutParameter(ctx.index(), Types.VARCHAR);
    }

    @Override
    public void set(BindingSetStatementContext<HashMap<String, String>> ctx) throws SQLException {
        ctx.statement().setString(ctx.index(), Objects.toString(ctx.convert(converter()).value(), null));
    }


    @Override
    public void get(BindingGetResultSetContext<HashMap<String, String>> ctx) throws SQLException {
        ctx.convert(converter()).value(ctx.resultSet().getString(ctx.index()));
    }

    @Override
    public void get(BindingGetStatementContext<HashMap<String, String>> ctx) throws SQLException {
        ctx.convert(converter()).value(ctx.statement().getString(ctx.index()));
    }


    //not needed for postgres

    @Override
    public void set(BindingSetSQLOutputContext<HashMap<String, String>> ctx) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void get(BindingGetSQLInputContext<HashMap<String, String>> ctx) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }
}
