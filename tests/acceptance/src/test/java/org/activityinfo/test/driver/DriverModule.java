package org.activityinfo.test.driver;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import org.activityinfo.test.config.ConfigurationError;
import org.activityinfo.test.driver.mail.EmailDriver;
import org.activityinfo.test.driver.mail.mailinator.MailinatorClient;
import org.activityinfo.test.driver.mail.postmark.PostmarkStubClient;
import org.activityinfo.test.driver.mail.postmark.PostmarkStubServer;

public class DriverModule extends AbstractModule {

    private final String driver;

    public DriverModule() {
        driver = System.getProperty("app.driver", "web");
    }

    public DriverModule(String driver) {
        this.driver = driver;
    }

    @Override
    protected void configure() {
        switch (driver) {
            case "api":
                bind(ApplicationDriver.class).to(ApiApplicationDriver.class);
                break;
            case "web":
                bind(ApplicationDriver.class).to(UiApplicationDriver.class);
                break;
            default:
                throw new ConfigurationError("Invalid value for system property -Dapp.driver. " +
                        "Must be either 'web' or 'api'");
        }
        
        if(PostmarkStubServer.POSTMARK_STUB_PORT.isPresent()) {
            bind(EmailDriver.class).to(PostmarkStubClient.class);
            
        } else {
            bind(EmailDriver.class).to(MailinatorClient.class);
        }
    }
}
