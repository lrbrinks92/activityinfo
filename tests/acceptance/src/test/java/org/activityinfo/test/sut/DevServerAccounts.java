package org.activityinfo.test.sut;

import com.codahale.metrics.Meter;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import org.activityinfo.test.capacity.Metrics;
import org.activityinfo.test.config.ConfigProperty;
import org.mindrot.bcrypt.BCrypt;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;


/**
 * Manages creation of users accounts on the local dev server
 */
public class DevServerAccounts implements Accounts {

    private static final Logger LOGGER = Logger.getLogger(DevServerAccounts.class.getName());


    private static final ConfigProperty DATABASE_URL = new ConfigProperty("databaseUrl", "MySQL database url");
    private static final ConfigProperty EMAIL = new ConfigProperty("devAccountEmail", "Dev account email");

    private static final String DEV_PASSWORD = "notasecret";
    public static final String DEV_PASSWORD_HASHED = BCrypt.hashpw(DEV_PASSWORD, BCrypt.gensalt());

    private final Meter users = Metrics.REGISTRY.meter("registeredUsers");

    /**
     * Map from test-friendly handles to unique email addresses
     */
    private LoadingCache<String, UserAccount> aliasMap;

    private Random random = new Random();
    private boolean batchingEnabled = false;
    private List<String> pendingUsers = Lists.newArrayList();
    private String locale = "en";

    public DevServerAccounts() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL driver is not on the classpath", e);
        }
        LOGGER.info("Using connection to " + connectionUrl());

        aliasMap = CacheBuilder.newBuilder().concurrencyLevel(100).build(new CacheLoader<String, UserAccount>() {
            @Override
            public UserAccount load(String key) throws Exception {
                return createUniqueAccount(key);
            }
        });
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public boolean isBatchingEnabled() {
        return batchingEnabled;
    }

    public void setBatchingEnabled(boolean batchingEnabled) {
        this.batchingEnabled = batchingEnabled;
    }

    @Override
    public UserAccount ensureAccountExists(String userName) {
        try {
            return aliasMap.get(userName);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Creates a unique account mapped to the username for the duration of the test.
     * This ensures that each test is isolated.
     *
     * @param userName a friendly test name actually used in tests
     * @return a UserAccount with a unique username and password for this user
     */
    private synchronized UserAccount createUniqueAccount(String userName) {

        String email = generateAlias(userName);

        LOGGER.fine(String.format("Creating account %s for user %s", email, userName));

        if(batchingEnabled) {
            pendingUsers.add(email);
        } else {
            insertUsers(Arrays.asList(email));
        }

        return new UserAccount(email, DEV_PASSWORD);
    }

    private Connection openConnection() throws SQLException {

        // Add all system properties prefixed by 'mysql.' to the driver properties,
        // stripped of the 'mysql.' prefix
        Properties properties = new Properties();
        for(String systemProperty : System.getProperties().stringPropertyNames()) {
            if(systemProperty.startsWith("mysql.")) {
                String key = systemProperty.substring("mysql.".length());
                String value = System.getProperty(systemProperty);
                properties.put(key, value);
            }
        }
        return DriverManager.getConnection(DATABASE_URL.get(), properties);
            
    }
    
    public void flush() {
        if(!pendingUsers.isEmpty()) {
            LOGGER.info(String.format("Creating %d accounts...", pendingUsers.size()));
            insertUsers(pendingUsers);
            pendingUsers.clear();
        }
    }

    private void insertUsers(List<String> users) {

        try(Connection connection = openConnection()) {
            connection.setAutoCommit(false);

            // Create the user for testing purposes
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO userlogin (email, name, password, locale) VALUES(?, ?, ?, ?)")) {

                for(String user : users) {
                    stmt.setString(1, user);
                    stmt.setString(2, nameForEmail(user));
                    stmt.setString(3, DEV_PASSWORD_HASHED);
                    stmt.setString(4, locale);
                    stmt.addBatch();

                    this.users.mark();
                }
                stmt.executeBatch();
            }

            connection.commit();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String generateAlias(String testHandle) {
        Preconditions.checkNotNull(testHandle, "testHandle");

        StringBuilder alias = new StringBuilder();

        // Add any alphabetic prefix from the test handle,
        // so that bob@bedatadiven.com becomes bob_xyzzsdff@mailinator.com
        // and is at least recognizable
        for(int i=0;i!=testHandle.length();++i) {
            char c = testHandle.toLowerCase().charAt(i);
            if(c >= 'a' && c <= 'z') {
                alias.append(c);
            } else {
                break;
            }
        }
        if(alias.length() == 0) {
            alias.append("user");
        }
        alias.append("_");
        alias.append(Long.toHexString(random.nextLong()));
        alias.append("@mailinator.com");

        return alias.toString();
    }

    private String connectionUrl() {
        return DATABASE_URL.getOr("jdbc:mysql://localhost/activityinfo_at?useUnicode=true&characterEncoding=UTF-8");
    }
    
    private String nameForEmail(String email) {
        int at = email.indexOf('@');
        return email.substring(0, at);
    }

    @Override
    public UserAccount any() {
        return ensureAccountExists("user");
    }
}
