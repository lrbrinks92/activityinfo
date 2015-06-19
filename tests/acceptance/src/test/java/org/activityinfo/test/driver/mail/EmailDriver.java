package org.activityinfo.test.driver.mail;

import org.activityinfo.test.sut.UserAccount;

import java.io.IOException;

/**
 * Support tests with the ability to read email notifications sent 
 */
public interface EmailDriver {

    UserAccount newAccount();
    
    NotificationEmail lastNotificationFor(UserAccount account) throws IOException;

    
}
