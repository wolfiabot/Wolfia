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

import net.ttddyy.dsproxy.listener.QueryCountStrategy;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.stereotype.Component;
import space.npstr.sqlsauce.DatabaseWrapper;
import space.npstr.wolfia.App;
import space.npstr.wolfia.config.properties.DatabaseConfig;
import space.npstr.wolfia.config.properties.WolfiaConfig;

import javax.annotation.concurrent.ThreadSafe;
import javax.persistence.EntityManager;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by napster on 26.04.18.
 * <p>
 * Serves lazily initialized database connection and wrapper singletons
 */
@ThreadSafe
@Component
public class Database {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Database.class);

    private final DatabaseConfig databaseConfig;
    private final WolfiaConfig wolfiaConfig;
    private final QueryCountStrategy queryCountStrategy;

    private final AtomicReference<space.npstr.sqlsauce.DatabaseConnection> connection = new AtomicReference<>();
    private final Object connectionInitLock = new Object();
    private final AtomicReference<DatabaseWrapper> wrapper = new AtomicReference<>();
    private final Object wrapperInitLock = new Object();

    public Database(final DatabaseConfig databaseConfig, final WolfiaConfig wolfiaConfig,
                    QueryCountStrategy queryCountStrategy) {

        this.databaseConfig = databaseConfig;
        this.wolfiaConfig = wolfiaConfig;
        this.queryCountStrategy = queryCountStrategy;
    }

    public EntityManager getEntityManager() {
        return getSqlSauceConnection().getEntityManager();
    }

    public int getMaxPoolSize() {
        return getSqlSauceConnection().getMaxPoolSize();
    }

    public DatabaseWrapper getWrapper() {
        DatabaseWrapper singleton = this.wrapper.get();
        if (singleton == null) {
            synchronized (this.wrapperInitLock) {
                singleton = this.wrapper.get();
                if (singleton == null) {
                    singleton = new DatabaseWrapper(getSqlSauceConnection());
                    this.wrapper.set(singleton);
                }
            }
        }
        return singleton;
    }

    private space.npstr.sqlsauce.DatabaseConnection getSqlSauceConnection() {
        space.npstr.sqlsauce.DatabaseConnection singleton = this.connection.get();
        if (singleton == null) {
            synchronized (this.connectionInitLock) {
                singleton = this.connection.get();
                if (singleton == null) {
                    singleton = initSqlSauceConnection();
                    this.connection.set(singleton);
                }
            }
        }
        return singleton;
    }

    public void shutdown() {
        synchronized (this.connectionInitLock) {
            final space.npstr.sqlsauce.DatabaseConnection conn = this.connection.get();
            if (conn != null) {
                conn.shutdown();
            }
        }
    }

    private space.npstr.sqlsauce.DatabaseConnection initSqlSauceConnection() {
        try {
            final Flyway flyway = new Flyway(new FluentConfiguration()
                    .locations("db/migrations"));

            return new space.npstr.sqlsauce.DatabaseConnection.Builder("postgres", this.databaseConfig.getJdbcUrl())
                    .setDialect("org.hibernate.dialect.PostgreSQL95Dialect")
                    .addEntityPackage("space.npstr.wolfia.db.entities")
                    .setAppName("Wolfia_" + (this.wolfiaConfig.isDebug() ? "DEBUG" : "PROD") + "_" + App.getVersionBuild())
                    .setHibernateProperty("hibernate.hbm2ddl.auto", "validate")
                    //hide some exception spam on start, as postgres does not support CLOBs
                    // https://stackoverflow.com/questions/43905119/postgres-error-method-org-postgresql-jdbc-pgconnection-createclob-is-not-imple
                    .setHibernateProperty("hibernate.jdbc.lob.non_contextual_creation", "true")
                    .setFlyway(flyway)
                    .setProxyDataSourceBuilder(new ProxyDataSourceBuilder()
                            .logSlowQueryBySlf4j(10, TimeUnit.SECONDS, SLF4JLogLevel.WARN, "SlowQueryLog")
                            .multiline()
                            .name("postgres")
                            .countQuery(this.queryCountStrategy)
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
