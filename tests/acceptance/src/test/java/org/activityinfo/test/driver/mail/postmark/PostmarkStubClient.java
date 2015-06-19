package org.activityinfo.test.driver.mail.postmark;

import org.activityinfo.test.driver.mail.EmailDriver;
import org.activityinfo.test.driver.mail.NotificationEmail;
import org.activityinfo.test.sut.UserAccount;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Long.toHexString;

public class PostmarkStubClient implements EmailDriver {

    public PostmarkStubClient() throws IOException {
        PostmarkStubServer.start();
    }

    @Override
    public UserAccount newAccount() {
        return new UserAccount(toHexString(ThreadLocalRandom.current().nextLong()) + "@example.com", "notasecret");
    }

    @Override
    public NotificationEmail lastNotificationFor(UserAccount account) throws IOException {
        for (Message sentMessage : PostmarkStubServer.SENT_MESSAGES) {
            if(sentMessage.getTo().equals(account.getEmail())) {
                return new NotificationEmail(sentMessage.getSubject(), sentMessage.getTextBody());
            }
        }
        throw new AssertionError("No notification email for " + account.getEmail());
    }
}
