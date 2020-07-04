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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.jooq.exception.DataAccessException;

public class DatabaseConnection {

    private final HikariDataSource hikariDataSource;
    private final ProxyDataSource proxiedDataSource;

    /**
     * @throws DataAccessException if the connection could not be created due to [reasons]
     */
    public DatabaseConnection(final HikariConfig hikariConfig,
                              final ProxyDataSourceBuilder proxyDataSourceBuilder,
                              final FluentConfiguration flywayConfig) {

        try {
            this.hikariDataSource = new HikariDataSource(hikariConfig);

            flywayConfig.dataSource(this.hikariDataSource);
            Flyway flyway = new Flyway(flywayConfig);
            flyway.repair();
            flyway.migrate();

            this.proxiedDataSource = proxyDataSourceBuilder
                    .dataSource(this.hikariDataSource)
                    .build();
        } catch (final Exception e) {
            throw new DataAccessException("Failed to create database connection", e);
        }
    }

    public DataSource getDataSource() {
        return this.proxiedDataSource;
    }

    public void shutdown() {
        this.hikariDataSource.close();
    }
}
