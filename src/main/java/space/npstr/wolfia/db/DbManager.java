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

import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.utils.App;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * Created by napster on 29.05.17.
 * <p>
 * Provides access to the underlying database(s)
 */
public class DbManager {

    private static final Logger log = LoggerFactory.getLogger(DbManager.class);

    private final EntityManagerFactory emf;

    public DbManager() {
        // hikari connection pool
        final HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(Config.C.jdbcUrl);
        ds.setMaximumPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        ds.setPoolName("Wolfia Default Pool");
        ds.setValidationTimeout(1000);
        ds.setConnectionTimeout(2000);
        ds.setConnectionTestQuery("SELECT 1;");
        final Properties props = new Properties();
        props.setProperty("ApplicationName", "Wolfia_" + (Config.C.isDebug ? "DEBUG" : "PROD") + "_" + App.VERSION);
        ds.setDataSourceProperties(props);

        // jpa
        final PersistenceUnitInfo puInfo = defaultPersistenceUnitInfo(ds, "space.npstr.wolfia.db.entity");

        // hibernate
        final Properties hibernateProps = new Properties();
        //this will work fine as long as we never change columns and only add them
        hibernateProps.put("hibernate.hbm2ddl.auto", "update");

        this.emf = new HibernatePersistenceProvider().createContainerEntityManagerFactory(puInfo, hibernateProps);
    }

    public EntityManager getEntityManager() {
        return this.emf.createEntityManager();
    }


    //copy pasta'd this from somewhere on stackoverflow, seems to work with slight adjustments
    private PersistenceUnitInfo defaultPersistenceUnitInfo(final DataSource ds, @SuppressWarnings("SameParameterValue") final String entityPackage) {
        return new PersistenceUnitInfo() {
            @Override
            public String getPersistenceUnitName() {
                return "ApplicationPersistenceUnit";
            }

            @Override
            public String getPersistenceProviderClassName() {
                return "org.hibernate.jpa.HibernatePersistenceProvider";
            }

            @Override
            public PersistenceUnitTransactionType getTransactionType() {
                return PersistenceUnitTransactionType.RESOURCE_LOCAL;
            }

            @Override
            public DataSource getJtaDataSource() {
                return ds;
            }

            @Override
            public DataSource getNonJtaDataSource() {
                return ds;
            }

            @Override
            public List<String> getMappingFileNames() {
                return Collections.emptyList();
            }

            @Override
            public List<URL> getJarFileUrls() {
                try {
                    return Collections.list(this.getClass()
                            .getClassLoader()
                            .getResources(""));
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public URL getPersistenceUnitRootUrl() {
                return null;
            }

            @Override
            public List<String> getManagedClassNames() {
                return getClassesForPackage(entityPackage).stream().map(Class::getName).collect(Collectors.toList());
            }

            @Override
            public boolean excludeUnlistedClasses() {
                return false;
            }

            @Override
            public SharedCacheMode getSharedCacheMode() {
                return null;
            }

            @Override
            public ValidationMode getValidationMode() {
                return null;
            }

            @Override
            public Properties getProperties() {
                return new Properties();
            }

            @Override
            public String getPersistenceXMLSchemaVersion() {
                return null;
            }

            @Override
            public ClassLoader getClassLoader() {
                return null;
            }

            @Override
            public void addTransformer(final ClassTransformer transformer) {
                //do nothing
            }

            @Override
            public ClassLoader getNewTempClassLoader() {
                return null;
            }
        };
    }


    //ugly code below this don't look please
    //https://stackoverflow.com/a/3527428
    // its only here to avoid the mistake of forgetting to manually add an entity class to the jpa managed classes
    // why, you ask? because I want to avoid using xml files to configure the database connection (no reason really, I
    // just want to know if it's possible), but at the same time I don't want to add spring or other frameworks who
    // allow xml free configuration (and have methods to add whole packages to be monitored for managed classes)

    private static List<Class<?>> getClassesForPackage(final String pkgName) {
        final List<Class<?>> classes = new ArrayList<>();
        // Get a File object for the package
        File directory;
        final String fullPath;
        final String relPath = pkgName.replace('.', '/');
        log.debug("ClassDiscovery: Package: " + pkgName + " becomes Path:" + relPath);
        final URL resource = ClassLoader.getSystemClassLoader().getResource(relPath);
        log.debug("ClassDiscovery: Resource = " + resource);
        if (resource == null) {
            throw new RuntimeException("No resource for " + relPath);
        }
        fullPath = resource.getFile();
        log.debug("ClassDiscovery: FullPath = " + resource);

        try {
            directory = new File(resource.toURI());
        } catch (final URISyntaxException e) {
            throw new RuntimeException(pkgName + " (" + resource + ") does not appear to be a valid URL / URI.  Strange, since we got it from the system...", e);
        } catch (final IllegalArgumentException e) {
            directory = null;
        }
        log.debug("ClassDiscovery: Directory = " + directory);

        if (directory != null && directory.exists()) {
            // Get the list of the files contained in the package
            final String[] files = directory.list();
            if (files != null) {
                for (final String file : files) {
                    // we are only interested in .class files
                    if (file.endsWith(".class")) {
                        // removes the .class extension
                        final String className = pkgName + '.' + file.substring(0, file.length() - 6);
                        log.debug("ClassDiscovery: className = " + className);
                        try {
                            classes.add(Class.forName(className));
                        } catch (final ClassNotFoundException e) {
                            throw new RuntimeException("ClassNotFoundException loading " + className);
                        }
                    }
                }
            }
        } else {
            try {
                final String jarPath = fullPath.replaceFirst("[.]jar[!].*", ".jar").replaceFirst("file:", "");
                final JarFile jarFile = new JarFile(jarPath);
                final Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    final JarEntry entry = entries.nextElement();
                    final String entryName = entry.getName();
                    if (entryName.startsWith(relPath) && entryName.length() > (relPath.length() + "/".length())) {
                        log.debug("ClassDiscovery: JarEntry: " + entryName);
                        final String className = entryName.replace('/', '.').replace('\\', '.').replace(".class", "");

                        //skip packages
                        if (className.endsWith(".")) {
                            continue;
                        }
                        //just a class
                        log.debug("ClassDiscovery: className = " + className);
                        try {
                            classes.add(Class.forName(className));
                        } catch (final ClassNotFoundException e) {
                            throw new RuntimeException("ClassNotFoundException loading " + className);
                        }

                    }
                }
            } catch (final IOException e) {
                throw new RuntimeException(pkgName + " (" + directory + ") does not appear to be a valid package", e);
            }
        }
        return classes;
    }
}
