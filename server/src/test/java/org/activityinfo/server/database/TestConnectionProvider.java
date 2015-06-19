package org.activityinfo.server.database;

/*
 * #%L
 * ActivityInfo Server
 * %%
 * Copyright (C) 2009 - 2013 UNICEF
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.inject.Provider;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Provides a connection to the test database.
 * <p/>
 * By default, we connect to the local 'activityinfo-test' database, with
 * username 'root' and password 'adminpwd'.
 * <p/>
 * This can be overridden by adding the 'testDatabaseUrl',
 * 'testDatabaseUsername' and 'testDatabasePassword' properties to the
 * activityinfo.properties file, or add them as system variables.
 */
public class TestConnectionProvider implements ConnectionProvider, Provider<Connection> {

    private static final Logger LOGGER = Logger.getLogger(TestConnectionProvider.class.getName());

    private static final String PASSWORD_PROPERTY = "testDatabasePassword";
    private static final String USERNAME_PROPERTY = "testDatabaseUsername";

    private static final String DEFAULT_PASSWORD = "adminpwd";
    private static final String DEFAULT_USERNAME = "root";
    
    public static String DATABASE_NAME, USERNAME, PASSWORD;
    
    
    private static ComboPooledDataSource POOL;
    

    static {
        try {

            Class.forName("com.mysql.jdbc.Driver");

            Properties activityinfoProperties = new Properties();
            File propertiesFile = new File(System.getProperty("user.home"), "activityinfo.properties");
            if (propertiesFile.exists()) {
                activityinfoProperties.load(new FileInputStream(propertiesFile));
            }
            
            String usernameProperty = activityinfoProperties.getProperty(USERNAME_PROPERTY);
            USERNAME = usernameProperty != null ? usernameProperty :
                    System.getProperty(USERNAME_PROPERTY, DEFAULT_USERNAME);

            String passwordProperty = activityinfoProperties.getProperty(PASSWORD_PROPERTY);
            PASSWORD = passwordProperty != null ? passwordProperty :
                    System.getProperty(PASSWORD_PROPERTY, DEFAULT_PASSWORD);


            // Gradle is set up to run tests across several forked JVMs
            // if the worker.id property is present, then automatically create a fresh
            // database and run liquibase

            String workerId = System.getProperty("org.gradle.test.worker");
            if(!Strings.isNullOrEmpty(workerId)) {
                DATABASE_NAME = "aitest_" + workerId;
            } else {

                DATABASE_NAME = System.getProperty("testDatabaseName");
                if (Strings.isNullOrEmpty(DATABASE_NAME)) {
                    throw new Error("No database name provided");
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Can't initialize TestConnectionProvider, error loading property file", e);
        }
    }
    
    static synchronized Connection openConnection() throws SQLException {
        if(POOL == null) {
            try {
                if(!Boolean.getBoolean("skipDatabaseInit")) {
                    initializeDatabase();
                }

                POOL = new ComboPooledDataSource();
                POOL.setDriverClass("com.mysql.jdbc.Driver");
                POOL.setJdbcUrl(connectionUrl(DATABASE_NAME));
                POOL.setUser(USERNAME);
                POOL.setPassword(PASSWORD);
                POOL.setMinPoolSize(2);
                POOL.setAcquireIncrement(1);
                POOL.setMaxPoolSize(5);
                POOL.setUnreturnedConnectionTimeout(60);
                POOL.setDebugUnreturnedConnectionStackTraces(true);
            } catch (Exception e) {
                throw new Error("Could not open connection to " + DATABASE_NAME, e);
            }
        }
        return POOL.getConnection();
    }

    @Override
    public Connection get() {
        try {
            return  openConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static String connectionUrl(String dbName) {
        return String.format("jdbc:mysql://localhost/%s?useUnicode=true&characterEncoding=UTF-8", dbName);
    }

    private static void initializeDatabase() throws SQLException, LiquibaseException {

        Connection connection = DriverManager.getConnection(connectionUrl(""), USERNAME, PASSWORD);
        Statement stmt = connection.createStatement();
        stmt.execute("DROP DATABASE IF EXISTS " + DATABASE_NAME);
        stmt.execute("CREATE DATABASE " + DATABASE_NAME);
        stmt.execute("USE " + DATABASE_NAME);

        Liquibase liquibase = new Liquibase("org/activityinfo/database/changelog/db.changelog-master.xml",
                new ClassLoaderResourceAccessor(),
                new JdbcConnection(connection));
        liquibase.update(null);
        connection.close();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return openConnection();
    }

    @Override
    public void closeConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class aClass) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> aClass) {
        return null;
    }
}
