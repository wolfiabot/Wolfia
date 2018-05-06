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

import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
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
@Slf4j
@ThreadSafe
public class Database {

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
            final Flyway flyway = new Flyway();
            flyway.setBaselineOnMigrate(true);
            flyway.setBaselineVersion(MigrationVersion.fromVersion("0"));
            flyway.setBaselineDescription("Base Migration");
            flyway.setLocations("classpath:space/npstr/wolfia/db/migrations");

            return new DatabaseConnection.Builder("postgres", Config.C.jdbcUrl)
                    .setDialect("org.hibernate.dialect.PostgreSQL95Dialect")
                    .addEntityPackage("space.npstr.wolfia.db.entities")
                    .setAppName("Wolfia_" + (Config.C.isDebug ? "DEBUG" : "PROD") + "_" + App.getVersionBuild())
                    .setHibernateProperty("hibernate.hbm2ddl.auto", "validate")
                    //hide some exception spam on start, as postgres does not support CLOBs
                    // https://stackoverflow.com/questions/43905119/postgres-error-method-org-postgresql-jdbc-pgconnection-createclob-is-not-imple
                    .setHibernateProperty("hibernate.jdbc.lob.non_contextual_creation", "true")
                    .setFlyway(flyway)
                    .setProxyDataSourceBuilder(new ProxyDataSourceBuilder()
                            .logSlowQueryBySlf4j(10, TimeUnit.SECONDS, SLF4JLogLevel.WARN, "SlowQueryLog")
                            .multiline()
                    )
                    .setEntityManagerFactoryBuilder((puName, dataSource, properties, entityPackages) -> {
                        LocalContainerEntityManagerFactoryBean emfb = new LocalContainerEntityManagerFactoryBean();
                        emfb.setDataSource(dataSource);
                        emfb.setPackagesToScan(entityPackages.toArray(new String[0]));

                        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
                        emfb.setJpaVendorAdapter(vendorAdapter);
                        emfb.setJpaProperties(properties);

                        emfb.afterPropertiesSet(); //initiate creation of the native emf
                        return emfb.getNativeEntityManagerFactory();
                    })
                    .build();
        } catch (final Exception e) {
            final String message = "Failed to set up database connection";
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
    }
}
