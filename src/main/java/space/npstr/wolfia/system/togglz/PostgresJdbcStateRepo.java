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

package space.npstr.wolfia.system.togglz;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.postgresql.PGConnection;
import org.postgresql.jdbc.AutoSave;
import org.togglz.core.repository.jdbc.JDBCStateRepository;
import space.npstr.wolfia.system.Exceptions;

/**
 * Make the {@link JDBCStateRepository} work with Postgres which might be anal about a query failing
 * (initial check whether table exists) and make subsequent queries fail too
 * Also we don't use autocommit so we need to commit when done.
 */
public class PostgresJdbcStateRepo extends JDBCStateRepository {

    private AutoSave initial = null;

    public PostgresJdbcStateRepo(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    protected void beforeSchemaMigration(Connection connection) {
        PGConnection pgConnection = unwrap(connection);
        this.initial = pgConnection.getAutosave();
        pgConnection.setAutosave(AutoSave.ALWAYS);
    }

    @Override
    protected void afterSchemaMigration(Connection connection) {
        if (this.initial == null) {
            throw new IllegalStateException("Did not record previous auto save setting of the connection.");
        }
        PGConnection pgConnection = unwrap(connection);
        pgConnection.setAutosave(this.initial);
        try {
            connection.commit();
        } catch (SQLException e) {
            Exceptions.sneakyThrow(e); //caller of this method actually handles it
        }
    }

    private PGConnection unwrap(Connection connection) {
        try {
            return connection.unwrap(PGConnection.class);
        } catch (SQLException e) {
            throw new IllegalStateException("Did we start using a different db driver than postgres?", e);
        }
    }

}
