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

import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.sqlsauce.DatabaseConnection;
import space.npstr.sqlsauce.DatabaseWrapper;
import space.npstr.wolfia.App;
import space.npstr.wolfia.Config;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.TimeUnit;

/**
 * Created by napster on 26.04.18.
 * <p>
 * Serves lazily initialized database connection and wrapper singletons
 */
@ThreadSafe
public class Database {

    private static final Logger log = LoggerFactory.getLogger(Database.class);

    @Nullable
    private volatile DatabaseConnection connection;
    private final Object connectionInitLock = new Object();
    @Nullable
    private volatile DatabaseWrapper wrapper;
    private final Object wrapperInitLock = new Object();

    public DatabaseWrapper getWrapper() {
        DatabaseWrapper singleton = this.wrapper;
        if (singleton == null) {
            synchronized (this.wrapperInitLock) {
                singleton = this.wrapper;
                if (singleton == null) {
                    this.wrapper = singleton = new DatabaseWrapper(getConnection());
                }
            }
        }
        return singleton;
    }

    public DatabaseConnection getConnection() {
        DatabaseConnection singleton = this.connection;
        if (singleton == null) {
            synchronized (this.connectionInitLock) {
                singleton = this.connection;
                if (singleton == null) {
                    this.connection = singleton = initConnection();
                }
            }
        }
        return singleton;
    }

    public void shutdown() {
        synchronized (this.connectionInitLock) {
            final DatabaseConnection conn = this.connection;
            if (conn != null) {
                conn.shutdown();
            }
        }
    }

    private static DatabaseConnection initConnection() {
        try {
            return new DatabaseConnection.Builder("postgres", Config.C.jdbcUrl)
                    .setDialect("org.hibernate.dialect.PostgreSQL95Dialect")
                    .addEntityPackage("space.npstr.wolfia.db.entities")
                    .setAppName("Wolfia_" + (Config.C.isDebug ? "DEBUG" : "PROD") + "_" + App.getVersionBuild())
//                    .addMigration(new m00001FixCharacterVaryingColumns())
//                    .addMigration(new m00002CachedUserToDiscordUser())
//                    .addMigration(new m00003EGuildToDiscordGuild())
                    .setProxyDataSourceBuilder(new ProxyDataSourceBuilder()
                            .logSlowQueryBySlf4j(10, TimeUnit.SECONDS, SLF4JLogLevel.WARN, "SlowQueryLog")
                            .multiline()
                    )
                    .build();
        } catch (final Exception e) {
            final String message = "Failed to set up database connection";
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
    }
}
