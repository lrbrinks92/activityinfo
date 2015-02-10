package org.activityinfo.test.sut;

import com.google.common.base.Strings;
import com.google.common.io.Resources;
import org.activityinfo.test.config.ConfigProperty;
import org.activityinfo.test.config.ConfigurationError;
import org.junit.internal.AssumptionViolatedException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * Allocates accounts on the live version of activityinfo.org using credentials
 * provided in an external properties file.
 */
public class PredefinedAccounts implements Accounts {

    public static final ConfigProperty CREDENTIALS_PROPERTY = new ConfigProperty(
            "user.credentials",
            "the path to a properties file containing a list of existing credentials in the format\n" +
                    "user@test.com=password");

    private final Properties credentials;

    public PredefinedAccounts() {
        credentials = new Properties();
        try(InputStream in = new FileInputStream(CREDENTIALS_PROPERTY.getFile())) {
            credentials.load(in);
        } catch (Exception e) {
            throw new ConfigurationError("Exception loading credentials property from " +
                    CREDENTIALS_PROPERTY.getFile().getAbsolutePath(), e);
        }
    }

    public PredefinedAccounts(Properties credentials) {
        this.credentials = credentials;
    }

    public static PredefinedAccounts fromResource(Class relativeToClass, String resourceName) {
        try {
            URL url = Resources.getResource(relativeToClass, resourceName);
            Properties properties = new Properties();
            try(InputStream in = Resources.asByteSource(url).openStream()) {
                properties.load(in);
            }
            return new PredefinedAccounts(properties);
        } catch (Exception e) {
            throw new ConfigurationError(String.format("Could not load credentials from resource '%s'", resourceName), e);
        }
    }

    @Override
    public UserAccount ensureAccountExists(String email) {
        String password = credentials.getProperty(email);
        if(Strings.isNullOrEmpty(password)) {
            throw new AssumptionViolatedException(String.format("The user account %s does not exist.", email));
        }
        return new UserAccount(email, password);
    }
}
